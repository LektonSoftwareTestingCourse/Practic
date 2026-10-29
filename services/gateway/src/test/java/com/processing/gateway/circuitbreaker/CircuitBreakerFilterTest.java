package com.processing.gateway.circuitbreaker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.downstream.DownstreamServiceResolver;
import com.processing.gateway.metrics.GatewayMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerFilterTest {

    private static final String PATH = "/api/transactions";
    private static final String SERVICE = "switch";

    @Mock
    private DownstreamServiceResolver serviceResolver;
    @Mock
    private InMemoryCircuitBreaker circuitBreaker;
    @Mock
    private GatewayFilterChain chain;
    @Mock
    private GatewayMetrics metrics;

    private ObjectMapper objectMapper;
    private CircuitBreakerFilter filter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new CircuitBreakerFilter(objectMapper, serviceResolver, circuitBreaker, metrics, Duration.ofSeconds(10));
    }

    @Test
    void shouldForwardRequestWhenNoDownstreamServiceMatches() {
        //Arrange
        MockServerWebExchange exchange = transactionExchange();
        when(serviceResolver.resolve(PATH)).thenReturn(Optional.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        //Act
        Mono<Void> result = filter.filter(exchange, chain);

        //Assert
        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(exchange);
        verifyNoInteractions(metrics);
    }

    @Test
    void shouldReturnServiceUnavailableWithoutForwardingWhenCircuitIsOpen() throws Exception {
        //Arrange
        MockServerWebExchange exchange = transactionExchange();
        when(serviceResolver.resolve(PATH)).thenReturn(Optional.of(SERVICE));
        when(circuitBreaker.allowRequest(SERVICE)).thenReturn(false);

        //Act
        Mono<Void> result = filter.filter(exchange, chain);

        //Assert
        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("10");
        JsonNode error = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(error.path("error").asText()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(error.path("serviceName").asText()).isEqualTo(SERVICE);
        assertThat(error.path("message").asText()).isEqualTo("switch service is temporarily unavailable");
        verify(metrics).recordCircuitOpen(SERVICE);
        verifyNoInteractions(chain);
    }

    @ParameterizedTest(name = "status={0}")
    @ValueSource(ints = {200, 201, 400, 401, 429})
    void shouldRecordSuccessForCompletedResponseWithoutServerFailure(int status) {
        //Arrange
        MockServerWebExchange exchange = transactionExchange();
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        stubAllowedRoute();
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        //Act
        Mono<Void> result = filter.filter(exchange, chain);

        //Assert
        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(exchange);
        verify(circuitBreaker).recordSuccess(SERVICE);
        verify(circuitBreaker, never()).recordFailure(anyString());
        verify(circuitBreaker, never()).releaseRequest(anyString());
    }

    @Test
    void shouldRecordSuccessWhenResponseStatusIsNotExplicitlySet() {
        //Arrange
        MockServerWebExchange exchange = transactionExchange();
        stubAllowedRoute();
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        //Act
        Mono<Void> result = filter.filter(exchange, chain);

        //Assert
        StepVerifier.create(result).verifyComplete();
        verify(circuitBreaker).recordSuccess(SERVICE);
        verify(circuitBreaker, never()).recordFailure(anyString());
    }

    @ParameterizedTest(name = "status={0}")
    @ValueSource(ints = {500, 502, 503, 504})
    void shouldRecordFailureForServerErrorResponse(int status) {
        //Arrange
        MockServerWebExchange exchange = transactionExchange();
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        stubAllowedRoute();
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        //Act
        Mono<Void> result = filter.filter(exchange, chain);

        //Assert
        StepVerifier.create(result).verifyComplete();
        verify(circuitBreaker).recordFailure(SERVICE);
        verify(circuitBreaker, never()).recordSuccess(anyString());
        verify(circuitBreaker, never()).releaseRequest(anyString());
    }

    @ParameterizedTest(name = "status={0}")
    @ValueSource(ints = {404, 405})
    void shouldReleaseProbeWithoutChangingFailureCountForUnmappedOperation(int status) {
        //Arrange
        MockServerWebExchange exchange = transactionExchange();
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        stubAllowedRoute();
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        //Act
        Mono<Void> result = filter.filter(exchange, chain);

        //Assert
        StepVerifier.create(result).verifyComplete();
        verify(circuitBreaker).releaseRequest(SERVICE);
        verify(circuitBreaker, never()).recordSuccess(anyString());
        verify(circuitBreaker, never()).recordFailure(anyString());
    }

    @ParameterizedTest(name = "failure={0}")
    @MethodSource("connectionFailures")
    void shouldRecordFailureAndPropagateDownstreamConnectionError(Throwable error) {
        //Arrange
        MockServerWebExchange exchange = transactionExchange();
        stubAllowedRoute();
        when(chain.filter(exchange)).thenReturn(Mono.error(error));

        //Act
        Mono<Void> result = filter.filter(exchange, chain);

        //Assert
        StepVerifier.create(result).expectErrorSatisfies(failure -> assertThat(failure).isSameAs(error)).verify();
        verify(circuitBreaker).recordFailure(SERVICE);
        verify(circuitBreaker, never()).recordSuccess(anyString());
        verify(circuitBreaker, never()).releaseRequest(anyString());
    }

    @Test
    void shouldReleaseProbeAndPropagateApplicationError() {
        //Arrange
        MockServerWebExchange exchange = transactionExchange();
        var error = new IllegalArgumentException("Invalid operation");
        stubAllowedRoute();
        when(chain.filter(exchange)).thenReturn(Mono.error(error));

        //Act
        Mono<Void> result = filter.filter(exchange, chain);

        //Assert
        StepVerifier.create(result).expectErrorSatisfies(failure -> assertThat(failure).isSameAs(error)).verify();
        verify(circuitBreaker).releaseRequest(SERVICE);
        verify(circuitBreaker, never()).recordSuccess(anyString());
        verify(circuitBreaker, never()).recordFailure(anyString());
    }

    private static Stream<Throwable> connectionFailures() {
        return Stream.of(
                new ResourceAccessException("Timeout"),
                new ConnectException("Connection refused"),
                new SocketTimeoutException("Read timeout"),
                new IllegalStateException("Wrapped error", new ConnectException("Connection refused")));
    }

    private void stubAllowedRoute() {
        when(serviceResolver.resolve(PATH)).thenReturn(Optional.of(SERVICE));
        when(circuitBreaker.allowRequest(SERVICE)).thenReturn(true);
    }

    private MockServerWebExchange transactionExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.post(PATH).build());
    }
}
