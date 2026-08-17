package com.processing.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.metrics.GatewayMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

/**
 * Applies the configured per-second in-memory limit to transaction requests.
 */
@Component
@RequiredArgsConstructor
public class TransactionRateLimitFilter implements GlobalFilter, Ordered {

    private static final String TRANSACTIONS_PATH = "/api/transactions";
    private static final String RATE_LIMIT_KEY_PREFIX = "POST " + TRANSACTIONS_PATH + ":";

    private final InMemoryRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;
    private final GatewayMetrics gatewayMetrics;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = clientIpResolver.resolve(exchange);
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + clientIp;

        if (!isTransactionRequest(exchange)
                || rateLimiter.allowRequest(rateLimitKey)) {
            return chain.filter(exchange);
        }

        gatewayMetrics.recordRateLimitRejected();

        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.RETRY_AFTER, "1");

        return writeJson(exchange, Map.of(
                "error", "RATE_LIMIT_EXCEEDED",
                "message", "Too many requests. Try again later.",
                "retryAfterMs", 1000
        ));
    }

    private boolean isTransactionRequest(ServerWebExchange exchange) {
        return HttpMethod.POST.equals(exchange.getRequest().getMethod())
                && TRANSACTIONS_PATH.equals(exchange.getRequest().getPath().value());
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
