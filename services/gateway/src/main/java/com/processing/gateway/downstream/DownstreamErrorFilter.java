package com.processing.gateway.downstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import com.processing.gateway.metrics.GatewayMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Optional;

/**
 * Converts downstream connectivity failures into gateway-level HTTP 503 responses.
 */
@Component
@RequiredArgsConstructor
public class DownstreamErrorFilter implements GlobalFilter, Ordered {
    private final ObjectMapper objectMapper;
    private final DownstreamServiceResolver serviceResolver;
    private final GatewayMetrics gatewayMetrics;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 4;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(throwable -> {
                    Optional<String> serviceName = serviceResolver.resolve(exchange.getRequest().getPath().value());

                    if (serviceName.isPresent()
                            && DownstreamExceptionUtils.isDownstreamUnavailable(throwable)
                            && !exchange.getResponse().isCommitted()) {
                        gatewayMetrics.recordDownstreamUnavailable(serviceName.get());
                        return writeServiceUnavailable(exchange, serviceName.get());
                    }

                    return Mono.error(throwable);
                });
    }

    private Mono<Void> writeServiceUnavailable(ServerWebExchange exchange, String serviceName) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return writeJson(exchange, new ServiceUnavailableResponse(
                "SERVICE_UNAVAILABLE",
                capitalize(serviceName) + " service is temporarily unavailable",
                serviceName
        ));
    }

    private String capitalize(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return serviceName;
        }

        return serviceName.substring(0, 1).toUpperCase() + serviceName.substring(1);
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
