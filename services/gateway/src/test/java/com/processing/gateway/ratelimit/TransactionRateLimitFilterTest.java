package com.processing.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.metrics.GatewayMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionRateLimitFilterTest {

    private static final String KEY = "POST /api/transactions:127.0.0.1";

    private final InMemoryRateLimiter rateLimiter = mock(InMemoryRateLimiter.class);
    private final ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
    private final GatewayMetrics metrics = mock(GatewayMetrics.class);
    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);
    private final TransactionRateLimitFilter filter = new TransactionRateLimitFilter(
            rateLimiter,
            clientIpResolver,
            new ObjectMapper(),
            metrics
    );

    @Test
    void shouldRejectRequestWhenLimitIsExceeded() {
        MockServerWebExchange exchange = transactionExchange();
        when(clientIpResolver.resolve(exchange)).thenReturn("127.0.0.1");
        when(rateLimiter.allowRequest(KEY)).thenReturn(false);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("RATE_LIMIT_EXCEEDED")
                .contains("Too many requests");
        verify(metrics).recordRateLimitRejected();
        verifyNoInteractions(chain);
    }

    @Test
    void shouldPassRequestWhenLimitIsNotExceeded() {
        MockServerWebExchange exchange = transactionExchange();
        when(clientIpResolver.resolve(exchange)).thenReturn("127.0.0.1");
        when(rateLimiter.allowRequest(KEY)).thenReturn(true);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(metrics);
    }

    @Test
    void shouldIgnoreRequestWhenPathIsNotTransaction() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/cards").build()
        );
        when(clientIpResolver.resolve(exchange)).thenReturn("127.0.0.1");
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    private static MockServerWebExchange transactionExchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/transactions").build()
        );
    }
}
