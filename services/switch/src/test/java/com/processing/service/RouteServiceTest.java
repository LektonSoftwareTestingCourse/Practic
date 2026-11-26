package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.exception.AuthorizationException;
import com.processing.exception.UnknownBinException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RouteServiceTest {
    private static final String PAN = "4000001234567890";
    private static final String STAN = "123456";
    private static final String RRN = "123456789012";

    private RoutingService routingService;
    private AuthorizationClient authorizationClient;
    private AcquiringFeeClient acquiringFeeClient;
    private LoggerClient loggerClient;
    private RouteService routeService;

    @BeforeEach
    void setUp() {
        routingService = mock(RoutingService.class);
        authorizationClient = mock(AuthorizationClient.class);
        acquiringFeeClient = mock(AcquiringFeeClient.class);
        loggerClient = mock(LoggerClient.class);
        routeService = new RouteService(routingService, authorizationClient, acquiringFeeClient, loggerClient);
    }

    @Test
    void shouldRouteApprovedPaymentAndPublishTransaction() {
        when(routingService.getIssuerIdByPan(PAN)).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(approved());
        when(acquiringFeeClient.fetchAcquiringFee(any(), eq(STAN), eq(PAN), eq("TERM1000"), any()))
                .thenReturn(new BigDecimal("2.50"));
        when(loggerClient.log(any())).thenReturn(true);

        AuthorizationResponse response = routeService.route(request());

        assertEquals(AuthorizationResponse.STATUS_APPROVED, response.status());
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(AuthorizationRequest.class);
        verify(authorizationClient).authorize(requestCaptor.capture());
        assertEquals("ISS001", requestCaptor.getValue().issuerId());
        assertEquals("TERM1000", requestCaptor.getValue().terminalId());
        assertEquals("MERCH0000000002", requestCaptor.getValue().merchantId());
        var transactionCaptor = org.mockito.ArgumentCaptor.forClass(TransactionRequest.class);
        verify(loggerClient).log(transactionCaptor.capture());
        assertEquals(TransactionStatus.APPROVED, transactionCaptor.getValue().status());
        assertEquals(new BigDecimal("2.50"), transactionCaptor.getValue().acquiringFee());
        verify(authorizationClient, never()).rollback(any(), any());
    }

    @Test
    void shouldDeclineUnknownBinAndStillLogTransaction() {
        when(routingService.getIssuerIdByPan(PAN)).thenThrow(new UnknownBinException("400000"));
        when(loggerClient.log(any())).thenReturn(true);

        AuthorizationResponse response = routeService.route(request());

        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_NOT_FOUND", response.declineReason());
        verifyNoInteractions(authorizationClient, acquiringFeeClient);
        verify(loggerClient).log(any(TransactionRequest.class));
    }

    @Test
    void shouldDeclineShortPanWithoutAttemptingSubstringRouting() {
        AuthorizationRequest shortPanRequest = requestWithPan("12345");
        when(routingService.getIssuerIdByPan("12345")).thenThrow(new UnknownBinException("null-or-short"));
        when(loggerClient.log(any())).thenReturn(true);

        AuthorizationResponse response = routeService.route(shortPanRequest);

        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        verifyNoInteractions(authorizationClient, acquiringFeeClient);
    }

    @Test
    void shouldHandleNullPanWithoutAttemptingSubstringRouting() {
        AuthorizationRequest nullPanRequest = requestWithPan(null);
        when(routingService.getIssuerIdByPan(null)).thenThrow(new UnknownBinException("null-or-short"));
        when(loggerClient.log(any())).thenReturn(true);

        AuthorizationResponse response = routeService.route(nullPanRequest);

        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        verifyNoInteractions(authorizationClient, acquiringFeeClient);
    }

    @Test
    void shouldReturnDeclinedResponseWhenAuthorizationIsUnavailable() {
        when(routingService.getIssuerIdByPan(PAN)).thenReturn("ISS001");
        when(authorizationClient.authorize(any()))
                .thenThrow(new AuthorizationException(STAN, 3, "timeout"));
        when(loggerClient.log(any())).thenReturn(true);

        AuthorizationResponse response = routeService.route(request());

        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("Authorization service unavailable", response.declineReason());
        verifyNoInteractions(acquiringFeeClient);
        verify(loggerClient).log(any(TransactionRequest.class));
    }

    @Test
    void shouldNotRollbackWhenLoggingFailsForDeclinedAuthorization() {
        when(routingService.getIssuerIdByPan(PAN)).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(declined());
        when(acquiringFeeClient.fetchAcquiringFee(any(), any(), any(), any(), any())).thenReturn(null);
        when(loggerClient.log(any())).thenReturn(false);

        AuthorizationResponse response = routeService.route(request());

        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        verify(authorizationClient, never()).rollback(any(), any());
    }

    @Test
    void shouldRollbackApprovedPaymentWhenLoggingFails() {
        when(routingService.getIssuerIdByPan(PAN)).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(approved());
        when(acquiringFeeClient.fetchAcquiringFee(any(), any(), any(), any(), any())).thenReturn(null);
        when(loggerClient.log(any())).thenReturn(false);
        when(authorizationClient.rollback(any(), eq(RRN))).thenReturn(
                new RollbackResponse(RRN, "00", RollbackResponse.STATUS_APPROVED, null, 1));

        AuthorizationResponse response = routeService.route(request());

        assertEquals("96", response.responseCode());
        verify(authorizationClient).rollback(any(), eq(RRN));
    }

    @Test
    void shouldLogSuccessfulRollbackWhenPublishFails() {
        when(routingService.getIssuerIdByPan(PAN)).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(approved());
        when(acquiringFeeClient.fetchAcquiringFee(any(), any(), any(), any(), any())).thenReturn(null);
        when(loggerClient.log(any())).thenReturn(false);
        when(authorizationClient.rollback(any(), eq(RRN))).thenReturn(
                new RollbackResponse(RRN, "00", RollbackResponse.STATUS_APPROVED, null, 1));

        AuthorizationResponse response = routeService.route(request());

        assertEquals("96", response.responseCode());
        verify(authorizationClient).rollback(any(), eq(RRN));
    }

    @Test
    void shouldHandleMissingRollbackResponseAfterPublishFailure() {
        when(routingService.getIssuerIdByPan(PAN)).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(approved());
        when(acquiringFeeClient.fetchAcquiringFee(any(), any(), any(), any(), any())).thenReturn(null);
        when(loggerClient.log(any())).thenReturn(false);
        when(authorizationClient.rollback(any(), eq(RRN))).thenReturn(null);

        assertEquals("96", routeService.route(request()).responseCode());
    }

    @Test
    void shouldHandleDeclinedRollbackResponseAfterPublishFailure() {
        when(routingService.getIssuerIdByPan(PAN)).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(approved());
        when(acquiringFeeClient.fetchAcquiringFee(any(), any(), any(), any(), any())).thenReturn(null);
        when(loggerClient.log(any())).thenReturn(false);
        when(authorizationClient.rollback(any(), eq(RRN))).thenReturn(
                new RollbackResponse(RRN, "05", RollbackResponse.STATUS_DECLINED, "conflict", 1));

        assertEquals("96", routeService.route(request()).responseCode());
    }

    private static AuthorizationRequest request() {
        return requestWithPan(PAN);
    }

    private static AuthorizationRequest requestWithPan(String pan) {
        return new AuthorizationRequest("0100", STAN, pan, "000000", new BigDecimal("100.00"), "643",
                Instant.parse("2026-11-12T10:00:00Z"), "TERM1", "POS", "MERCH0000000002", "5045", "ACQ001", null);
    }

    private static AuthorizationResponse approved() {
        return new AuthorizationResponse("0110", STAN, RRN, "A1B2C3", "00", "APPROVED", null, null);
    }

    private static AuthorizationResponse declined() {
        return new AuthorizationResponse("0110", STAN, null, null, "05", "DECLINED", "CARD_BLOCKED", 3);
    }
}
