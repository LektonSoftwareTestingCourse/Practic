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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    private static final String PAN = "4000001234567890";
    private static final String UNKNOWN_PAN = "9999991234567890";
    private static final String STAN = "000001";
    private static final String ISSUER_ID = "ISS001";
    private static final String RRN = "123456789012";
    private static final String AUTH_CODE = "AUTH01";
    private static final BigDecimal AMOUNT = new BigDecimal("1000");
    private static final BigDecimal ACQUIRING_FEE = new BigDecimal("25");
    private static final Instant TX_TIME = Instant.parse("2026-01-01T10:00:00Z");

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
        routeService = new RouteService(
                routingService,
                authorizationClient,
                acquiringFeeClient,
                loggerClient);
    }

    @Test
    void shouldRouteTransactionAndPublishToLoggerWhenAuthorizationIsApproved() {
        AuthorizationResponse approvedResponse = approvedResponse(25);
        when(routingService.getIssuerIdByPan(PAN)).thenReturn(ISSUER_ID);
        when(authorizationClient.authorize(any(AuthorizationRequest.class)))
                .thenReturn(approvedResponse);
        when(acquiringFeeClient.fetchAcquiringFee(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(ACQUIRING_FEE);
        when(loggerClient.log(any(TransactionRequest.class))).thenReturn(true);

        AuthorizationResponse response = routeService.route(request(PAN));

        assertThat(response).isEqualTo(approvedResponse);

        ArgumentCaptor<AuthorizationRequest> authorizationRequest =
                ArgumentCaptor.forClass(AuthorizationRequest.class);
        verify(authorizationClient).authorize(authorizationRequest.capture());
        assertThat(authorizationRequest.getValue().issuerId()).isEqualTo(ISSUER_ID);

        ArgumentCaptor<TransactionRequest> transaction = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(loggerClient).log(transaction.capture());
        assertThat(transaction.getValue().issuerId()).isEqualTo(ISSUER_ID);
        assertThat(transaction.getValue().acquiringFee()).isEqualByComparingTo(ACQUIRING_FEE);
        assertThat(transaction.getValue().status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(transaction.getValue().processingTimeMs()).isEqualTo(25);
        assertThat(transaction.getValue().declineReason()).isNull();
        verify(authorizationClient, never()).rollback(any(AuthorizationRequest.class), anyString());
    }

    @Test
    void shouldDeclineWithCode14WhenBinIsUnknown() {
        when(routingService.getIssuerIdByPan(UNKNOWN_PAN))
                .thenThrow(new UnknownBinException("999999"));
        when(loggerClient.log(any(TransactionRequest.class))).thenReturn(true);

        AuthorizationResponse response = routeService.route(request(UNKNOWN_PAN));

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_CARD_NOT_FOUND);
        assertThat(response.declineReason()).isEqualTo("CARD_NOT_FOUND");
        verifyNoInteractions(authorizationClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345"})
    void shouldDeclineWithCode14WhenPanIsNullOrTooShort(String pan) {
        when(routingService.getIssuerIdByPan(pan))
                .thenThrow(new UnknownBinException("null-or-short"));
        when(loggerClient.log(any(TransactionRequest.class))).thenReturn(true);

        AuthorizationResponse response = routeService.route(request(pan));

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_CARD_NOT_FOUND);
        verifyNoInteractions(authorizationClient);
    }

    @Test
    void shouldDeclineWithCode05WhenAuthorizationServiceIsUnavailable() {
        when(routingService.getIssuerIdByPan(PAN)).thenReturn(ISSUER_ID);
        when(authorizationClient.authorize(any(AuthorizationRequest.class)))
                .thenThrow(new AuthorizationException(STAN, 3, "connection refused"));
        when(loggerClient.log(any(TransactionRequest.class))).thenReturn(true);

        AuthorizationResponse response = routeService.route(request(PAN));

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_DECLINED_GENERAL);
        assertThat(response.declineReason()).isEqualTo("Authorization service unavailable");
        verify(authorizationClient, never()).rollback(any(AuthorizationRequest.class), anyString());
    }

    @ParameterizedTest
    @MethodSource("rollbackResponses")
    void shouldRollbackAndReturnSystemErrorWhenPublishFailsForApproved(RollbackResponse rollbackResponse) {
        when(routingService.getIssuerIdByPan(PAN)).thenReturn(ISSUER_ID);
        when(authorizationClient.authorize(any(AuthorizationRequest.class)))
                .thenReturn(approvedResponse(25));
        when(acquiringFeeClient.fetchAcquiringFee(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(ACQUIRING_FEE);
        when(loggerClient.log(any(TransactionRequest.class))).thenReturn(false);
        when(authorizationClient.rollback(any(AuthorizationRequest.class), eq(RRN)))
                .thenReturn(rollbackResponse);

        AuthorizationResponse response = routeService.route(request(PAN));

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_SERVICE_UNAVAILABLE);
        assertThat(response.declineReason()).isEqualTo("System error");
        verify(authorizationClient).rollback(any(AuthorizationRequest.class), eq(RRN));
    }

    @Test
    void shouldNotRollbackWhenPublishFailsForDeclinedTransaction() {
        AuthorizationResponse declinedResponse = declinedResponse();
        when(routingService.getIssuerIdByPan(PAN)).thenReturn(ISSUER_ID);
        when(authorizationClient.authorize(any(AuthorizationRequest.class)))
                .thenReturn(declinedResponse);
        when(loggerClient.log(any(TransactionRequest.class))).thenReturn(false);

        AuthorizationResponse response = routeService.route(request(PAN));

        assertThat(response).isEqualTo(declinedResponse);
        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo("51");
        verify(authorizationClient, never()).rollback(any(AuthorizationRequest.class), anyString());

        ArgumentCaptor<TransactionRequest> transaction = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(loggerClient).log(transaction.capture());
        assertThat(transaction.getValue().status()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(transaction.getValue().acquiringFee()).isNull();
        assertThat(transaction.getValue().processingTimeMs()).isZero();
    }

    private static Stream<Arguments> rollbackResponses() {
        return Stream.of(
                Arguments.of((RollbackResponse) null),
                Arguments.of(new RollbackResponse(RRN, "00", RollbackResponse.STATUS_APPROVED, null, 5)),
                Arguments.of(new RollbackResponse(RRN, "05", RollbackResponse.STATUS_DECLINED, "DECLINED", 5))
        );
    }

    private static AuthorizationResponse approvedResponse(int processingTimeMs) {
        return new AuthorizationResponse(
                "0110",
                STAN,
                RRN,
                AUTH_CODE,
                AuthorizationResponse.CODE_APPROVED,
                AuthorizationResponse.STATUS_APPROVED,
                null,
                processingTimeMs
        );
    }

    private static AuthorizationResponse declinedResponse() {
        return new AuthorizationResponse(
                "0110",
                STAN,
                null,
                null,
                "51",
                AuthorizationResponse.STATUS_DECLINED,
                "INSUFFICIENT_FUNDS",
                null
        );
    }

    private static AuthorizationRequest request(String pan) {
        return AuthorizationRequest.builder()
                .mti("0100")
                .stan(STAN)
                .pan(pan)
                .processingCode("000000")
                .amount(AMOUNT)
                .currencyCode("643")
                .transmissionDateTime(TX_TIME)
                .terminalId("TERM0001")
                .terminalType("POS")
                .merchantId("MERCH0000000002")
                .mcc("5045")
                .acquirerId("ACQ002")
                .issuerId(null)
                .build();
    }
}
