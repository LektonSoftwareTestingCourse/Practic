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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RoutingService routingService;
    @Mock
    private AuthorizationClient authorizationClient;
    @Mock
    private AcquiringFeeClient acquiringFeeClient;
    @Mock
    private LoggerClient loggerClient;

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteService(routingService, authorizationClient, acquiringFeeClient, loggerClient);
    }

    @Test
    void shouldRouteApprovedTransactionAndPublishCompleteLogRecord() {
        // Arrange
        AuthorizationRequest request = request();
        AuthorizationResponse approved = approvedResponse();
        when(routingService.getIssuerIdByPan(request.pan())).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(approved);
        when(acquiringFeeClient.fetchAcquiringFee(any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("12.50"));
        when(loggerClient.log(any())).thenReturn(true);

        // Act
        AuthorizationResponse result = routeService.route(request);

        // Assert
        assertEquals(approved, result);
        ArgumentCaptor<TransactionRequest> transactionCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(loggerClient).log(transactionCaptor.capture());
        TransactionRequest transaction = transactionCaptor.getValue();
        assertEquals(TransactionStatus.APPROVED, transaction.status());
        assertEquals("ISS001", transaction.issuerId());
        assertEquals(new BigDecimal("12.50"), transaction.acquiringFee());
        assertEquals("TERM0000", transaction.terminalId());
        assertEquals("MERCH0000000000", transaction.merchantId());
        verify(authorizationClient, never()).rollback(any(), any());
    }

    @Test
    void shouldDeclineUnknownBinAndStillPublishTransaction() {
        // Arrange
        AuthorizationRequest request = request();
        when(routingService.getIssuerIdByPan(request.pan())).thenThrow(new UnknownBinException("400000"));
        when(loggerClient.log(any())).thenReturn(true);

        // Act
        AuthorizationResponse result = routeService.route(request);

        // Assert
        assertEquals(AuthorizationResponse.CODE_CARD_NOT_FOUND, result.responseCode());
        assertEquals(AuthorizationResponse.STATUS_DECLINED, result.status());
        verifyNoInteractions(acquiringFeeClient);
        verify(loggerClient).log(any());
    }

    @Test
    void shouldDeclineWhenAuthorizationServiceIsUnavailable() {
        // Arrange
        AuthorizationRequest request = request();
        when(routingService.getIssuerIdByPan(request.pan())).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenThrow(new AuthorizationException(request.stan(), 3, "timeout"));
        when(loggerClient.log(any())).thenReturn(true);

        // Act
        AuthorizationResponse result = routeService.route(request);

        // Assert
        assertEquals(AuthorizationResponse.STATUS_DECLINED, result.status());
        assertEquals(AuthorizationResponse.CODE_DECLINED_GENERAL, result.responseCode());
        verifyNoInteractions(acquiringFeeClient);
    }

    @Test
    void shouldRollbackApprovedTransactionWhenLoggerRejectsMessage() {
        // Arrange
        AuthorizationRequest request = request();
        when(routingService.getIssuerIdByPan(request.pan())).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(approvedResponse());
        when(loggerClient.log(any())).thenReturn(false);
        when(authorizationClient.rollback(any(), any())).thenReturn(
                new RollbackResponse("012345678901", "00", "APPROVED", null, 1));

        // Act
        AuthorizationResponse result = routeService.route(request);

        // Assert
        assertEquals(AuthorizationResponse.CODE_SERVICE_UNAVAILABLE, result.responseCode());
        verify(authorizationClient).rollback(any(), any());
    }

    @Test
    void shouldHandleMissingRollbackResponseWhenLoggerRejectsMessage() {
        // Arrange
        AuthorizationRequest request = request();
        when(routingService.getIssuerIdByPan(request.pan())).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(approvedResponse());
        when(loggerClient.log(any())).thenReturn(false);
        when(authorizationClient.rollback(any(), any())).thenReturn(null);

        // Act
        AuthorizationResponse result = routeService.route(request);

        // Assert
        assertEquals(AuthorizationResponse.CODE_SERVICE_UNAVAILABLE, result.responseCode());
        assertNull(result.rrn());
    }

    @Test
    void shouldNotRollbackDeclinedTransactionWhenLoggerRejectsMessage() {
        // Arrange
        AuthorizationRequest request = request();
        AuthorizationResponse declined = AuthorizationResponse.unknownBin(request.stan());
        when(routingService.getIssuerIdByPan(request.pan())).thenReturn("ISS001");
        when(authorizationClient.authorize(any())).thenReturn(declined);
        when(loggerClient.log(any())).thenReturn(false);

        // Act
        AuthorizationResponse result = routeService.route(request);

        // Assert
        assertEquals(declined, result);
        verify(authorizationClient, never()).rollback(any(), any());
    }

    private AuthorizationRequest request() {
        return new AuthorizationRequest(
                "0100", "000001", "4000001234567890", "000000", new BigDecimal("1000"),
                "643", Instant.parse("2026-10-01T10:15:30Z"), "TERM", "POS",
                "MERCH00000000000", "5045", "ACQ002", null);
    }

    private AuthorizationResponse approvedResponse() {
        return new AuthorizationResponse(
                "0110", "000001", "012345678901", "ABC123", "00", "APPROVED", null, null);
    }
}
