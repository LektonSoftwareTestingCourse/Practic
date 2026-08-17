package com.processing.gateway.circuitbreaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import com.processing.gateway.downstream.DownstreamExceptionUtils;
import com.processing.gateway.downstream.DownstreamServiceResolver;
import com.processing.gateway.metrics.GatewayMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Reactive filter that protects downstream routes with a per-service circuit breaker.
 */
@Component
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;
    private final DownstreamServiceResolver serviceResolver;
    private final InMemoryCircuitBreaker circuitBreaker;
    private final GatewayMetrics gatewayMetrics;
    private final Duration openDuration;

    /**
     * Creates the filter with its collaborators and configured open duration.
     *
     * @param objectMapper mapper used to write JSON error bodies
     * @param serviceResolver resolves request paths to downstream services
     * @param circuitBreaker in-memory circuit breaker
     * @param openDuration duration advertised through {@code Retry-After}
     */
    public CircuitBreakerFilter(ObjectMapper objectMapper,
                                DownstreamServiceResolver serviceResolver,
                                InMemoryCircuitBreaker circuitBreaker,
                                GatewayMetrics gatewayMetrics,
                                @Value("${gateway.circuit-breaker.open-duration:10s}") Duration openDuration) {
        this.objectMapper = objectMapper;
        this.serviceResolver = serviceResolver;
        this.circuitBreaker = circuitBreaker;
        this.gatewayMetrics = gatewayMetrics;
        this.openDuration = openDuration;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Optional<String> serviceName = serviceResolver.resolve(exchange.getRequest().getPath().value());

        if (serviceName.isEmpty()) {
            return chain.filter(exchange);
        }

        String downstreamService = serviceName.get();
        if (!circuitBreaker.allowRequest(downstreamService)) {
            gatewayMetrics.recordCircuitOpen(downstreamService);
            return writeCircuitOpen(exchange, downstreamService);
        }

        return chain.filter(exchange)
                .doOnSuccess(ignored -> recordStatus(downstreamService, exchange.getResponse().getStatusCode()))
                .doOnError(throwable -> {
                    if (DownstreamExceptionUtils.isDownstreamUnavailable(throwable)) {
                        circuitBreaker.recordFailure(downstreamService);
                    } else {
                        circuitBreaker.releaseRequest(downstreamService);
                    }
                });
    }

    private Mono<Void> writeCircuitOpen(ServerWebExchange exchange, String serviceName) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(openDuration.toSeconds()));

        return writeJson(exchange, new ServiceUnavailableResponse(
                "SERVICE_UNAVAILABLE",
                serviceName + " service is temporarily unavailable",
                serviceName
        ));
    }

    private void recordStatus(String downstreamService, HttpStatusCode statusCode) {
        if (statusCode == null) {
            circuitBreaker.recordSuccess(downstreamService);
            return;
        }

        int status = statusCode.value();
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            circuitBreaker.recordFailure(downstreamService);
            return;
        }
        if (status == HttpStatus.NOT_FOUND.value()
                || status == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            circuitBreaker.releaseRequest(downstreamService);
            return;
        }

        circuitBreaker.recordSuccess(downstreamService);
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, Object body) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (IOException e) {
            return Mono.error(e);
        }
    }
}
