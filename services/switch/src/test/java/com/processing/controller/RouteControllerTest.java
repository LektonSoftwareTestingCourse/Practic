package com.processing.controller;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteControllerTest {

    @Mock
    private RouteService routeService;

    private RouteController routeController;

    @BeforeEach
    void setUp() {
        routeController = new RouteController(routeService);
    }

    @Test
    void shouldRouteTransactionWhenRequestIsValid() {
        AuthorizationRequest request = request("4000001234567890");
        AuthorizationResponse response = approvedResponse();
        when(routeService.route(request)).thenReturn(response);

        ResponseEntity<AuthorizationResponse> result = routeController.routeTransaction(request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(routeService).route(request);
    }

    @Test
    void shouldRouteTransactionWhenPanIsTooShort() {
        AuthorizationRequest request = request("1234567");
        AuthorizationResponse response = approvedResponse();
        when(routeService.route(request)).thenReturn(response);

        ResponseEntity<AuthorizationResponse> result = routeController.routeTransaction(request);

        assertThat(result.getBody()).isEqualTo(response);
        verify(routeService).route(request);
    }

    @Test
    void shouldRouteTransactionWhenPanIsNull() {
        AuthorizationRequest request = request(null);
        AuthorizationResponse response = approvedResponse();
        when(routeService.route(request)).thenReturn(response);

        ResponseEntity<AuthorizationResponse> result = routeController.routeTransaction(request);

        assertThat(result.getBody()).isEqualTo(response);
        verify(routeService).route(request);
    }

    private static AuthorizationResponse approvedResponse() {
        return new AuthorizationResponse(
                "0110",
                "000001",
                "123456789012",
                "AUTH01",
                "00",
                AuthorizationResponse.STATUS_APPROVED,
                null,
                10
        );
    }

    private static AuthorizationRequest request(String pan) {
        return AuthorizationRequest.builder()
                .mti("0100")
                .stan("000001")
                .pan(pan)
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
