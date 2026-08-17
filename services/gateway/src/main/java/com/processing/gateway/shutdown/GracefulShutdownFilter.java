package com.processing.gateway.shutdown;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import com.processing.gateway.metrics.GatewayMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GracefulShutdownFilter implements GlobalFilter, Ordered {
    private static final String GATEWAY_SERVICE_NAME = "gateway";

    private final GracefulShutdownState shutdownState;
    private final ShutdownProperties shutdownProperties;
    private final ObjectMapper objectMapper;
    private final GatewayMetrics gatewayMetrics;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!shutdownState.isShuttingDown()) {
            return chain.filter(exchange);
        }

        gatewayMetrics.recordGracefulShutdownRejected();

        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(shutdownProperties.retryAfterSeconds()));

        return writeJson(exchange, new ServiceUnavailableResponse(
                "SERVICE_UNAVAILABLE",
                "Gateway is shutting down, retry later",
                GATEWAY_SERVICE_NAME
        ));
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
