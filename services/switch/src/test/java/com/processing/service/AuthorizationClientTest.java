package com.processing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.config.SwitchProperties;
import com.processing.exception.AuthorizationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthorizationClientTest {

    private static final String URL = "http://authorization";
    private static final String AUTHORIZE_URL = URL + "/api/internal/authorize";
    private static final String ROLLBACK_URL = URL + "/api/internal/rollback";
    private static final String HEALTH_URL = URL + "/health";
    private static final String RRN = "123456789012";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer server;
    private AuthorizationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        Retry retry = Retry.of("test", RetryConfig.custom()
                .maxAttempts(1)
                .build());
        CircuitBreaker circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowSize(1)
                .minimumNumberOfCalls(1)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());

        client = new AuthorizationClient(properties(), builder.build(), retry, circuitBreaker);
    }

    @Test
    void shouldAuthorizeTransactionWhenResponseIsApproved() {
        AuthorizationResponse expected = authorizationResponse(
                AuthorizationResponse.STATUS_APPROVED,
                AuthorizationResponse.CODE_APPROVED);
        server.expect(requestTo(AUTHORIZE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json(expected), MediaType.APPLICATION_JSON));

        AuthorizationResponse actual = client.authorize(request());

        assertThat(actual).isEqualTo(expected);
        server.verify();
    }

    @Test
    void shouldAuthorizeTransactionWhenResponseIsDeclined() {
        AuthorizationResponse expected = authorizationResponse(
                AuthorizationResponse.STATUS_DECLINED,
                "51");
        server.expect(requestTo(AUTHORIZE_URL))
                .andRespond(withSuccess(json(expected), MediaType.APPLICATION_JSON));

        AuthorizationResponse actual = client.authorize(request());

        assertThat(actual).isEqualTo(expected);
        server.verify();
    }

    @Test
    void shouldThrowWhenAuthorizationStatusIsInvalid() {
        server.expect(requestTo(AUTHORIZE_URL))
                .andRespond(withSuccess(json(invalidResponse()), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.authorize(request()))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Invalid response from Authorization");
        server.verify();
    }

    @Test
    void shouldThrowWhenAuthorizationResponseIsEmpty() {
        server.expect(requestTo(AUTHORIZE_URL))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> client.authorize(request()))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Invalid response from Authorization");
        server.verify();
    }

    @Test
    void shouldThrowWhenAuthorizationServerReturnsError() {
        server.expect(requestTo(AUTHORIZE_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.authorize(request()))
                .isInstanceOf(AuthorizationException.class);
        server.verify();
    }

    @Test
    void shouldUseCauseMessageWhenConnectionFails() {
        server.expect(requestTo(AUTHORIZE_URL))
                .andRespond(withException(new IOException("connection reset")));

        assertThatThrownBy(() -> client.authorize(request()))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("connection reset");
        server.verify();
    }

    @Test
    void shouldThrowWhenCircuitBreakerIsOpen() {
        openCircuitBreaker();

        assertThatThrownBy(() -> client.authorize(request()))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Circuit breaker open");
    }

    @Test
    void shouldRollbackTransactionWhenResponseIsApproved() {
        RollbackResponse expected = new RollbackResponse(
                RRN, "00", RollbackResponse.STATUS_APPROVED, null, 5);
        server.expect(requestTo(ROLLBACK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json(expected), MediaType.APPLICATION_JSON));

        RollbackResponse actual = client.rollback(request(), RRN);

        assertThat(actual).isEqualTo(expected);
        server.verify();
    }

    @Test
    void shouldReturnDeclinedRollbackWhenResponseIsDeclined() {
        RollbackResponse expected = new RollbackResponse(
                RRN, "05", RollbackResponse.STATUS_DECLINED, "DECLINED", 5);
        server.expect(requestTo(ROLLBACK_URL))
                .andRespond(withSuccess(json(expected), MediaType.APPLICATION_JSON));

        RollbackResponse actual = client.rollback(request(), RRN);

        assertThat(actual).isEqualTo(expected);
        server.verify();
    }

    @Test
    void shouldReturnNullWhenRollbackStatusIsInvalid() {
        RollbackResponse invalid = new RollbackResponse(RRN, "05", "PENDING", null, 5);
        server.expect(requestTo(ROLLBACK_URL))
                .andRespond(withSuccess(json(invalid), MediaType.APPLICATION_JSON));

        RollbackResponse actual = client.rollback(request(), RRN);

        assertThat(actual).isNull();
        server.verify();
    }

    @Test
    void shouldReturnNullWhenRollbackResponseIsEmpty() {
        server.expect(requestTo(ROLLBACK_URL))
                .andRespond(withSuccess());

        RollbackResponse actual = client.rollback(request(), RRN);

        assertThat(actual).isNull();
        server.verify();
    }

    @Test
    void shouldReturnNullWhenRollbackRequestFails() {
        server.expect(requestTo(ROLLBACK_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        RollbackResponse actual = client.rollback(request(), RRN);

        assertThat(actual).isNull();
        server.verify();
    }

    @Test
    void shouldReturnOkWhenHealthCheckSucceeds() {
        server.expect(requestTo(HEALTH_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        assertThat(client.checkHealth()).isEqualTo("ok");
        server.verify();
    }

    private void openCircuitBreaker() {
        server.expect(requestTo(AUTHORIZE_URL))
                .andRespond(withSuccess(json(invalidResponse()), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.authorize(request()))
                .isInstanceOf(AuthorizationException.class);
        server.verify();
    }

    private String json(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AuthorizationResponse authorizationResponse(String status, String code) {
        return new AuthorizationResponse(
                "0110",
                "000001",
                RRN,
                "AUTH01",
                code,
                status,
                null,
                25
        );
    }

    private static AuthorizationResponse invalidResponse() {
        return new AuthorizationResponse(
                "0110",
                "000001",
                null,
                null,
                "05",
                "PENDING",
                null,
                0
        );
    }

    private static SwitchProperties properties() {
        SwitchProperties.RetryProperties retry =
                new SwitchProperties.RetryProperties(1, List.of());
        SwitchProperties.CircuitBreakerProperties circuitBreaker =
                new SwitchProperties.CircuitBreakerProperties(50, 1, 1, 60_000);
        return new SwitchProperties(
                "1.0.0",
                Map.of(),
                URL,
                null,
                null,
                null,
                retry,
                new SwitchProperties.CircuitBreakerSection(circuitBreaker));
    }

    private static AuthorizationRequest request() {
        return AuthorizationRequest.builder()
                .mti("0100")
                .stan("000001")
                .pan("4000001234567890")
                .processingCode("000000")
                .amount(new BigDecimal("1000"))
                .currencyCode("643")
                .transmissionDateTime(Instant.parse("2026-01-01T10:00:00Z"))
                .terminalId("TERM0001")
                .terminalType("POS")
                .merchantId("MERCH0000000002")
                .mcc("5045")
                .acquirerId("ACQ002")
                .issuerId("ISS001")
                .build();
    }
}
