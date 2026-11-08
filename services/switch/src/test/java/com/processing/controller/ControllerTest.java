package com.processing.controller;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.config.SwitchProperties;
import com.processing.dto.HealthResponse;
import com.processing.service.AuthorizationClient;
import com.processing.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllerTest {

    @Mock
    private RouteService routeService;
    @Mock
    private AuthorizationClient authorizationClient;

    private RouteController routeController;

    @BeforeEach
    void setUp() {
        routeController = new RouteController(routeService);
    }

    @Test
    void shouldReturnRouteServiceResponseForValidPan() {
        // Arrange
        AuthorizationRequest request = request("4000001234567890");
        AuthorizationResponse expected = AuthorizationResponse.unknownBin(request.stan());
        when(routeService.route(request)).thenReturn(expected);

        // Act
        ResponseEntity<AuthorizationResponse> response = routeController.routeTransaction(request);

        // Assert
        assertEquals(expected, response.getBody());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldAcceptMissingPanForControllerLevelLogging(String pan) {
        // Arrange
        AuthorizationRequest request = request(pan);
        AuthorizationResponse expected = AuthorizationResponse.unknownBin(request.stan());
        when(routeService.route(request)).thenReturn(expected);

        // Act
        ResponseEntity<AuthorizationResponse> response = routeController.routeTransaction(request);

        // Assert
        assertEquals(expected, response.getBody());
    }

    @Test
    void shouldReturnHealthWithAuthorizationDependency() {
        // Arrange
        when(authorizationClient.checkHealth()).thenReturn("ok");
        HealthController healthController = new HealthController(properties(), authorizationClient);

        // Act
        ResponseEntity<HealthResponse> response = healthController.health();

        // Assert
        assertEquals("ok", response.getBody().status());
        assertEquals("ok", response.getBody().dependencies().get("authorization"));
    }

    private AuthorizationRequest request(String pan) {
        return new AuthorizationRequest(
                "0100", "000001", pan, "000000", new BigDecimal("1000"), "643",
                Instant.parse("2026-10-01T10:15:30Z"), "TERM0001", "POS",
                "MERCH0000000001", "5045", "ACQ002", null);
    }

    private SwitchProperties properties() {
        return new SwitchProperties(
                "1.0", Map.of("400000", "ISS001"), "http://authorization", "http://logger",
                "http://merchant", new SwitchProperties.HttpProperties(100, 100, 100),
                new SwitchProperties.RetryProperties(1, List.of(1L)),
                new SwitchProperties.CircuitBreakerSection(
                        new SwitchProperties.CircuitBreakerProperties(50, 10, 5, 1000)));
    }
}
