package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.config.SwitchProperties;
import com.processing.exception.AuthorizationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpStatus;

class AuthorizationClientTest {
    private static final String AUTH_URL = "http://authorization.test";
    private static final String PAN = "4000001234567890";

    @Test
    void shouldReturnApprovedAuthorizationResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuthorizationClient client = client(builder.build(), closedCircuit());
        server.expect(requestTo(AUTH_URL + "/api/internal/authorize"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(authorizationJson("APPROVED"), MediaType.APPLICATION_JSON));

        AuthorizationResponse response = client.authorize(request());

        assertEquals("APPROVED", response.status());
        server.verify();
    }

    @Test
    void shouldReturnDeclinedAuthorizationResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuthorizationClient client = client(builder.build(), closedCircuit());
        server.expect(requestTo(AUTH_URL + "/api/internal/authorize"))
                .andRespond(withSuccess(authorizationJson("DECLINED"), MediaType.APPLICATION_JSON));

        AuthorizationResponse response = client.authorize(request());

        assertEquals("DECLINED", response.status());
        server.verify();
    }

    @Test
    void shouldThrowAuthorizationExceptionWhenAuthorizationBodyIsEmpty() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuthorizationClient client = client(builder.build(), closedCircuit());
        server.expect(requestTo(AUTH_URL + "/api/internal/authorize")).andRespond(withSuccess());

        assertThrows(AuthorizationException.class, () -> client.authorize(request()));
        server.verify();
    }

    @Test
    void shouldThrowAuthorizationExceptionWhenResponseStatusIsInvalid() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuthorizationClient client = client(builder.build(), closedCircuit());
        server.expect(requestTo(AUTH_URL + "/api/internal/authorize"))
                .andRespond(withSuccess(authorizationJson("UNKNOWN"), MediaType.APPLICATION_JSON));

        assertThrows(AuthorizationException.class, () -> client.authorize(request()));
        server.verify();
    }

    @Test
    void shouldReturnApprovedRollbackResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuthorizationClient client = client(builder.build(), closedCircuit());
        server.expect(requestTo(AUTH_URL + "/api/internal/rollback"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"rrn\":\"123456789012\",\"responseCode\":\"00\","
                        + "\"status\":\"APPROVED\",\"declineReason\":null,\"processingTimeMs\":1}",
                        MediaType.APPLICATION_JSON));

        RollbackResponse response = client.rollback(request(), "123456789012");

        assertEquals(RollbackResponse.STATUS_APPROVED, response.status());
        server.verify();
    }

    @Test
    void shouldReturnDeclinedRollbackResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuthorizationClient client = client(builder.build(), closedCircuit());
        server.expect(requestTo(AUTH_URL + "/api/internal/rollback"))
                .andRespond(withSuccess("{\"rrn\":\"123456789012\",\"responseCode\":\"05\","
                        + "\"status\":\"DECLINED\",\"declineReason\":\"CONFLICT\","
                        + "\"processingTimeMs\":1}", MediaType.APPLICATION_JSON));

        RollbackResponse response = client.rollback(request(), "123456789012");

        assertEquals(RollbackResponse.STATUS_DECLINED, response.status());
        server.verify();
    }

    @Test
    void shouldReturnNullWhenRollbackBodyIsEmptyOrHasUnknownStatus() {
        RestClient.Builder emptyBuilder = RestClient.builder();
        MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(emptyBuilder).build();
        AuthorizationClient emptyClient = client(emptyBuilder.build(), closedCircuit());
        emptyServer.expect(requestTo(AUTH_URL + "/api/internal/rollback")).andRespond(withSuccess());
        assertNull(emptyClient.rollback(request(), "123456789012"));
        emptyServer.verify();

        RestClient.Builder invalidBuilder = RestClient.builder();
        MockRestServiceServer invalidServer = MockRestServiceServer.bindTo(invalidBuilder).build();
        AuthorizationClient invalidClient = client(invalidBuilder.build(), closedCircuit());
        invalidServer.expect(requestTo(AUTH_URL + "/api/internal/rollback"))
                .andRespond(withSuccess("{\"rrn\":\"123456789012\",\"responseCode\":\"05\","
                        + "\"status\":\"UNKNOWN\",\"declineReason\":null,\"processingTimeMs\":1}",
                        MediaType.APPLICATION_JSON));
        assertNull(invalidClient.rollback(request(), "123456789012"));
        invalidServer.verify();
    }

    @Test
    void shouldReturnNullWhenRollbackEndpointFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuthorizationClient client = client(builder.build(), closedCircuit());
        server.expect(requestTo(AUTH_URL + "/api/internal/rollback"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertNull(client.rollback(request(), "123456789012"));
        server.verify();
    }

    @Test
    void shouldReportHealthAsOkOrDown() {
        RestClient.Builder okBuilder = RestClient.builder();
        MockRestServiceServer okServer = MockRestServiceServer.bindTo(okBuilder).build();
        AuthorizationClient okClient = client(okBuilder.build(), closedCircuit());
        okServer.expect(requestTo(AUTH_URL + "/health")).andRespond(withSuccess());
        assertEquals("ok", okClient.checkHealth());
        okServer.verify();

        RestClient.Builder downBuilder = RestClient.builder();
        MockRestServiceServer downServer = MockRestServiceServer.bindTo(downBuilder).build();
        AuthorizationClient downClient = client(downBuilder.build(), closedCircuit());
        downServer.expect(requestTo(AUTH_URL + "/health")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertEquals("down", downClient.checkHealth());
        downServer.verify();
    }

    @Test
    void shouldFailFastWhenCircuitBreakerIsOpen() {
        CircuitBreaker circuitBreaker = CircuitBreaker.of("authorization-test", CircuitBreakerConfig.custom()
                .slidingWindowSize(1)
                .minimumNumberOfCalls(1)
                .failureRateThreshold(50)
                .build());
        circuitBreaker.onError(0, TimeUnit.MILLISECONDS, new IllegalStateException("failed"));
        AuthorizationClient client = client(RestClient.builder().build(), circuitBreaker);

        assertThrows(AuthorizationException.class, () -> client.authorize(request()));
    }

    private static AuthorizationClient client(RestClient restClient, CircuitBreaker circuitBreaker) {
        SwitchProperties properties = new SwitchProperties(
                "test", Map.of(), AUTH_URL, "http://logger", "http://merchant",
                new SwitchProperties.HttpProperties(100, 100, 100),
                new SwitchProperties.RetryProperties(1, List.of(0L)),
                new SwitchProperties.CircuitBreakerSection(
                        new SwitchProperties.CircuitBreakerProperties(50, 10, 5, 1000)));
        Retry retry = Retry.of("authorization-retry-test", RetryConfig.custom()
                .maxAttempts(1).waitDuration(Duration.ZERO).build());
        return new AuthorizationClient(properties, restClient, retry, circuitBreaker);
    }

    private static CircuitBreaker closedCircuit() {
        return CircuitBreaker.of("authorization-test", CircuitBreakerConfig.ofDefaults());
    }

    private static AuthorizationRequest request() {
        return new AuthorizationRequest("0100", "123456", PAN, "000000", new BigDecimal("100.00"), "643",
                Instant.parse("2026-11-12T10:00:00Z"), "TERM0001", "POS", "MERCH0000000002", "5045", "ACQ001", "ISS001");
    }

    private static String authorizationJson(String status) {
        return "{\"mti\":\"0110\",\"stan\":\"123456\",\"rrn\":\"123456789012\","
                + "\"authCode\":\"A1B2C3\",\"responseCode\":\"00\",\"status\":\"" + status + "\","
                + "\"declineReason\":null,\"processingTimeMs\":1}";
    }
}
