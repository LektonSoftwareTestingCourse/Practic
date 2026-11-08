package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.config.SwitchProperties;
import com.processing.exception.AuthorizationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthorizationClientTest {

    private MockRestServiceServer server;
    private AuthorizationClient client;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        circuitBreaker = CircuitBreaker.ofDefaults("authorization-test");
        Retry retry = Retry.of("authorization-test", RetryConfig.custom()
                .maxAttempts(1)
                .build());
        client = new AuthorizationClient(properties(), builder.build(), retry, circuitBreaker);
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "DECLINED"})
    void shouldReturnValidAuthorizationResponse(String status) {
        // Arrange
        server.expect(once(), requestTo("http://authorization/api/internal/authorize"))
                .andExpect(method(POST))
                .andRespond(withSuccess(authorizationJson(status), MediaType.APPLICATION_JSON));

        // Act
        AuthorizationResponse response = client.authorize(request());

        // Assert
        assertEquals(status, response.status());
        server.verify();
    }

    @Test
    void shouldRejectUnexpectedAuthorizationStatus() {
        // Arrange
        server.expect(once(), requestTo("http://authorization/api/internal/authorize"))
                .andRespond(withSuccess(authorizationJson("PENDING"), MediaType.APPLICATION_JSON));

        // Act and Assert
        assertThrows(AuthorizationException.class, () -> client.authorize(request()));
        server.verify();
    }

    @Test
    void shouldRejectEmptyAuthorizationResponse() {
        // Arrange
        server.expect(once(), requestTo("http://authorization/api/internal/authorize"))
                .andRespond(withSuccess());

        // Act and Assert
        assertThrows(AuthorizationException.class, () -> client.authorize(request()));
        server.verify();
    }

    @Test
    void shouldRejectCallWhenCircuitBreakerIsOpen() {
        // Arrange
        circuitBreaker.transitionToOpenState();

        // Act and Assert
        assertThrows(AuthorizationException.class, () -> client.authorize(request()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "DECLINED"})
    void shouldReturnValidRollbackResponse(String status) {
        // Arrange
        server.expect(once(), requestTo("http://authorization/api/internal/rollback"))
                .andExpect(method(POST))
                .andRespond(withSuccess(rollbackJson(status), MediaType.APPLICATION_JSON));

        // Act
        RollbackResponse response = client.rollback(request(), "012345678901");

        // Assert
        assertEquals(status, response.status());
        server.verify();
    }

    @Test
    void shouldReturnNullForInvalidRollbackResponse() {
        // Arrange
        server.expect(once(), requestTo("http://authorization/api/internal/rollback"))
                .andRespond(withSuccess());

        // Act
        RollbackResponse response = client.rollback(request(), "012345678901");

        // Assert
        assertNull(response);
        server.verify();
    }

    @Test
    void shouldReportHealthyAuthorizationService() {
        // Arrange
        server.expect(once(), requestTo("http://authorization/health"))
                .andExpect(method(GET))
                .andRespond(withSuccess());

        // Act
        String health = client.checkHealth();

        // Assert
        assertEquals("ok", health);
        server.verify();
    }

    @Test
    void shouldReportUnavailableAuthorizationService() {
        // Arrange
        server.expect(once(), requestTo("http://authorization/health"))
                .andRespond(withBadRequest());

        // Act
        String health = client.checkHealth();

        // Assert
        assertEquals("down", health);
        server.verify();
    }

    private AuthorizationRequest request() {
        return new AuthorizationRequest(
                "0100", "000001", "4000001234567890", "000000", new BigDecimal("1000"),
                "643", Instant.parse("2026-10-01T10:15:30Z"), "TERM0001", "POS",
                "MERCH0000000001", "5045", "ACQ002", "ISS001");
    }

    private SwitchProperties properties() {
        return new SwitchProperties(
                "1.0", Map.of("400000", "ISS001"), "http://authorization", "http://logger",
                "http://merchant", new SwitchProperties.HttpProperties(100, 100, 100),
                new SwitchProperties.RetryProperties(1, List.of(1L)),
                new SwitchProperties.CircuitBreakerSection(
                        new SwitchProperties.CircuitBreakerProperties(50, 10, 5, 1000)));
    }

    private String authorizationJson(String status) {
        return """
                {
                  "mti": "0110",
                  "stan": "000001",
                  "rrn": "012345678901",
                  "authCode": "ABC123",
                  "responseCode": "00",
                  "status": "%s",
                  "processingTimeMs": 1
                }
                """.formatted(status);
    }

    private String rollbackJson(String status) {
        return """
                {
                  "rrn": "012345678901",
                  "responseCode": "00",
                  "status": "%s",
                  "processingTimeMs": 1
                }
                """.formatted(status);
    }
}
