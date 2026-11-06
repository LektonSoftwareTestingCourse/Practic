package com.processing.authorization.services;

import com.processing.authorization.client.*;
import com.processing.authorization.events.*;
import com.processing.authorization.exceptions.*;
import com.processing.authorization.repositories.*;
import com.processing.common.dto.authorization.*;
import com.processing.common.dto.cardmanagement.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.dao.*;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {
    private static final String PAN = "4000001234567890";
    private static final String BIN = "400000";
    private static final String ISSUER_ID = "ISS001";
    private static final BigDecimal AMOUNT = new BigDecimal("1000");
    private final Instant requestTime = Instant.now();
    private final YearMonth activeExpiry =
            YearMonth.from(requestTime.atZone(ZoneOffset.UTC)).plusYears(1);
    private final YearMonth expiredExpiry =
            YearMonth.from(requestTime.atZone(ZoneOffset.UTC)).minusMonths(1);

    @Mock
    private LimitUsageRepository limitUsageRepository;

    @Mock
    private AuthorizationEventNotifier eventNotifier;

    @Mock
    private BinLookupClient binLookupClient;

    @Mock
    private CardManagementClient cardManagementClient;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                limitUsageRepository,
                eventNotifier,
                binLookupClient,
                cardManagementClient);
    }

    @Test
    void shouldApproveAuthorizationWhenCardIsActiveAndSufficientFunds() {
        CardModel card = activeCard();
        when(cardManagementClient.getCard(PAN)).thenReturn(card);
        when(binLookupClient.getIssuerId(PAN)).thenReturn(Optional.of(ISSUER_ID));
        when(limitUsageRepository.upsertLimitUsage(any(), any(), any(), any(), any())).thenReturn(1);

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.rrn()).isNotBlank();
        assertThat(response.authCode()).isNotBlank();
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        verify(cardManagementClient).reserve(eq(AMOUNT), anyString(), eq(PAN));
        verify(eventNotifier).notify(any(AuthServiceAuthorizeEvent.class));
    }

    @Test
    void shouldGenerateDifferentRrnWhenCalledTwice() {
        when(limitUsageRepository.fetchRrnBlock()).thenReturn(1L);

        String first = authService.generateRRN();
        String second = authService.generateRRN();

        assertThat(first).matches("\\d{12}");
        assertThat(second).matches("\\d{12}");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldDeclineWhenCardNotFound() {
        when(cardManagementClient.getCard(PAN)).thenThrow(new CardNotFoundException(PAN));

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo("CARD_NOT_FOUND");
        assertThat(response.responseCode()).isEqualTo("14");
    }

    @ParameterizedTest
    @CsvSource({
            "BLOCKED,  CARD_BLOCKED,  05",
            "INACTIVE, CARD_INACTIVE, 05",
            "EXPIRED,  CARD_EXPIRED,  54"
    })
    void shouldDeclineWhenCardIsNotActive(CardModelStatus status, String reason, String code) {
        when(cardManagementClient.getCard(PAN))
                .thenReturn(card(status, activeExpiry, new BigDecimal("100000")));

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(reason);
        assertThat(response.responseCode()).isEqualTo(code);
    }

    @Test
    void shouldDeclineWhenCardExpiredByDate() {
        when(cardManagementClient.getCard(PAN)).thenReturn(expiredByDateCard());

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo("CARD_EXPIRED");
        assertThat(response.responseCode()).isEqualTo("54");
    }

    @Test
    void shouldDeclineWhenInsufficientFunds() {
        when(cardManagementClient.getCard(PAN))
                .thenReturn(cardWithBalance(new BigDecimal("10000")));

        AuthorizationResponse response = authService.authorize(
                request(new BigDecimal("10001")),
                requestTime
        );

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(response.responseCode()).isEqualTo("51");
    }

    @Test
    void shouldDeclineWhenLimitsExceeded() {
        when(cardManagementClient.getCard(PAN))
                .thenReturn(cardWithBalance(new BigDecimal("200000")));
        when(limitUsageRepository.upsertLimitUsage(any(), any(), any(), any(), any()))
                .thenReturn(0);

        AuthorizationResponse response = authService.authorize(
                request(new BigDecimal("150001")),
                requestTime
        );

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo("EXCEEDS_AMOUNT_LIMIT");
        assertThat(response.responseCode()).isEqualTo("61");
    }

    @Test
    void shouldApproveWhenAmountEqualsDailyLimit() {
        when(cardManagementClient.getCard(PAN))
                .thenReturn(cardWithBalance(new BigDecimal("200000")));
        when(limitUsageRepository.upsertLimitUsage(any(), any(), any(), any(), any()))
                .thenReturn(1);

        AuthorizationResponse response = authService.authorize(
                request(new BigDecimal("150000")),
                requestTime
        );

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
    }

    @Test
    void shouldRetryLimitUpdateWhenDuplicateKeyOccurs() {
        when(cardManagementClient.getCard(PAN)).thenReturn(activeCard());
        when(limitUsageRepository.upsertLimitUsage(any(), any(), any(), any(), any()))
                .thenThrow(new DuplicateKeyException("duplicate"))
                .thenReturn(1);

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
    }

    @Test
    void shouldApproveWhenBinLookupReturnsEmpty() {
        when(cardManagementClient.getCard(PAN)).thenReturn(activeCard());
        when(binLookupClient.getIssuerId(PAN)).thenReturn(Optional.empty());
        when(limitUsageRepository.upsertLimitUsage(any(), any(), any(), any(), any()))
                .thenReturn(1);

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
    }

    @ParameterizedTest
    @MethodSource("cardLookupFailures")
    void shouldDeclineWhenCardLookupFails(RuntimeException exception, String reason, String code) {
        when(cardManagementClient.getCard(PAN)).thenThrow(exception);

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(reason);
        assertThat(response.responseCode()).isEqualTo(code);
    }

    private static Stream<Arguments> cardLookupFailures() {
        return Stream.of(
                Arguments.of(new ServiceUnavailableException("cms down"), "SERVICE_UNAVAILABLE", "96"),
                Arguments.of(new PaymentRequiredException("payment required"), "CARD_BLOCKED", "05"),
                Arguments.of(new GetCardException("bad response"), "SERVICE_UNAVAILABLE", "96"),
                Arguments.of(new RuntimeException("unexpected"), "UNKNOWN_REASON", "05")
        );
    }

    @Test
    void shouldDeclineWhenCardStatusIsNull() {
        when(cardManagementClient.getCard(PAN))
                .thenReturn(card(null, activeExpiry, new BigDecimal("100000")));

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo("UNKNOWN_REASON");
    }

    @Test
    void shouldDeclineWhenLimitRetryFails() {
        when(cardManagementClient.getCard(PAN)).thenReturn(activeCard());
        when(limitUsageRepository.upsertLimitUsage(any(), any(), any(), any(), any()))
                .thenThrow(new DuplicateKeyException("duplicate"))
                .thenThrow(new DuplicateKeyException("duplicate again"));

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo("DB_UNAVAILABLE");
    }

    @ParameterizedTest
    @MethodSource("reserveFailures")
    void shouldDeclineWhenReserveFails(RuntimeException exception, String reason) {
        when(cardManagementClient.getCard(PAN)).thenReturn(activeCard());
        when(limitUsageRepository.upsertLimitUsage(any(), any(), any(), any(), any()))
                .thenReturn(1);

        doThrow(exception)
                .when(cardManagementClient)
                .reserve(any(), anyString(), anyString());

        AuthorizationResponse response = authService.authorize(validRequest(), requestTime);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(reason);
    }

    private static Stream<Arguments> reserveFailures() {
        return Stream.of(
                Arguments.of(new CardNotFoundException("missing"), "CARD_NOT_FOUND"),
                Arguments.of(new InvalidReserveRequestException("bad request"), "CARD_NOT_FOUND"),
                Arguments.of(new ServiceUnavailableException("cms down"), "SERVICE_UNAVAILABLE"),
                Arguments.of(new InsufficientFundsException("no money"), "INSUFFICIENT_FUNDS"),
                Arguments.of(new ReserveException("reserve failed"), "RESERVATION_FAILED"),
                Arguments.of(new RuntimeException("unexpected"), "UNKNOWN_REASON")
        );
    }

    @Test
    void shouldRollbackTransactionWhenRollbackIsRequested() {
        RollbackResponse response = authService.rollback(rollbackRequest(), requestTime);

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(RollbackResponse.CODE_SUCCESS);

        verify(cardManagementClient).rollback(rollbackRequest());
        verify(eventNotifier).notify(any(AuthServiceRollbackEvent.class));
    }

    @ParameterizedTest
    @MethodSource("rollbackFailures")
    void shouldDeclineRollbackWhenCmsFails(RuntimeException exception, String reason) {
        doThrow(exception)
                .when(cardManagementClient)
                .rollback(any());

        RollbackResponse response = authService.rollback(rollbackRequest(), requestTime);

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(reason);
    }

    private static Stream<Arguments> rollbackFailures() {
        return Stream.of(
                Arguments.of(new CardNotFoundException("missing"), "TRANSACTION_NOT_FOUND"),
                Arguments.of(new InvalidRollbackRequestException("bad request"), "TRANSACTION_NOT_FOUND"),
                Arguments.of(new ServiceUnavailableException("cms down"), "SERVICE_UNAVAILABLE"),
                Arguments.of(new RollbackConflictException("already rolled back"), "ALREADY_ROLLED_BACK"),
                Arguments.of(new RollbackFailureException("rollback failed"), "ROLLBACK_FAILED"),
                Arguments.of(new RuntimeException("unexpected"), "UNKNOWN_REASON")
        );
    }

    private AuthorizationRequest validRequest() {
        return request(AMOUNT);
    }

    private AuthorizationRequest request(BigDecimal amount) {
        return new AuthorizationRequest(
                "0100", "000001", PAN, "000000", amount, "643", requestTime,
                "TERM0001", "POS", "MERCH0000000002", "5045", "ACQ002", ISSUER_ID
        );
    }

    private CardModel activeCard() {
        return card(CardModelStatus.ACTIVE, activeExpiry, new BigDecimal("100000"));
    }

    private CardModel expiredByDateCard() {
        return card(CardModelStatus.ACTIVE, expiredExpiry, new BigDecimal("100000"));
    }

    private CardModel cardWithBalance(BigDecimal balance) {
        return card(CardModelStatus.ACTIVE, activeExpiry, balance);
    }

    private CardModel card(CardModelStatus status, YearMonth expiry, BigDecimal balance) {
        return new CardModel(
                UUID.randomUUID(), PAN, BIN, "IVAN IVANOV", expiry, status,
                "643", new BigDecimal("150000"), new BigDecimal("3000000"),
                balance, ISSUER_ID, requestTime
        );
    }

    private RollbackRequest rollbackRequest() {
        return new RollbackRequest("012345678901", PAN, AMOUNT);
    }
}
