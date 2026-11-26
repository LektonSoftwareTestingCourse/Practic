package com.processing.controller;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.service.RouteService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteControllerTest {
    @Test
    void shouldReturnSuccessfulHttpResponseForRoutedTransaction() {
        RouteService routeService = mock(RouteService.class);
        RouteController controller = new RouteController(routeService);
        AuthorizationRequest request = request("4000001234567890");
        AuthorizationResponse expected = new AuthorizationResponse("0110", "123456", null, null,
                "05", "DECLINED", "CARD_BLOCKED", 1);
        when(routeService.route(request)).thenReturn(expected);

        var response = controller.routeTransaction(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void shouldAlsoHandleShortPanWhenMaskingLogValue() {
        RouteService routeService = mock(RouteService.class);
        RouteController controller = new RouteController(routeService);
        AuthorizationRequest request = request("123");
        AuthorizationResponse expected = new AuthorizationResponse("0110", "123456", null, null,
                "14", "DECLINED", "CARD_NOT_FOUND", 1);
        when(routeService.route(request)).thenReturn(expected);

        assertEquals(expected, controller.routeTransaction(request).getBody());
    }

    @Test
    void shouldAlsoHandleNullPanWhenMaskingLogValue() {
        RouteService routeService = mock(RouteService.class);
        RouteController controller = new RouteController(routeService);
        AuthorizationRequest request = request(null);
        AuthorizationResponse expected = new AuthorizationResponse("0110", "123456", null, null,
                "14", "DECLINED", "CARD_NOT_FOUND", 1);
        when(routeService.route(request)).thenReturn(expected);

        assertEquals(expected, controller.routeTransaction(request).getBody());
    }

    private static AuthorizationRequest request(String pan) {
        return new AuthorizationRequest("0100", "123456", pan, "000000", BigDecimal.ONE, "643",
                Instant.EPOCH, "TERM0001", "POS", "MERCH0000000002", "5045", "ACQ001", null);
    }
}
