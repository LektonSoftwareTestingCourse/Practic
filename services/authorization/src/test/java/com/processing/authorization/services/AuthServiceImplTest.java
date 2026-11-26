package com.processing.authorization.services;

import com.processing.authorization.client.BinLookupClient;
import com.processing.authorization.client.CardManagementClient;
import com.processing.authorization.events.AuthorizationEventNotifier;
import com.processing.authorization.exceptions.CardNotFoundException;
import com.processing.authorization.exceptions.RollbackConflictException;
import com.processing.authorization.repositories.LimitUsageRepository;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    private static final String PAN = "4000001234567890";
    private static final Instant TRANSACTION_TIME = Instant.parse("2026-11-12T10:00:00Z");
    private static final Instant REQUEST_TIME = Instant.parse("2026-11-12T09:59:59Z");
    private static final BigDecimal AMOUNT = new BigDecimal("1000");

    @Mock private LimitUsageRepository limitUsageRepository;
    @Mock private AuthorizationEventNotifier eventNotifier;
    @Mock private BinLookupClient binLookupClient;
    @Mock private CardManagementClient cardManagementClient;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(limitUsageRepository, eventNotifier, binLookupClient, cardManagementClient);
    }

    @Test
    void shouldApproveWhenCardIsActiveAndAllChecksPass() {
        when(cardManagementClient.getCard(PAN)).thenReturn(card(CardModelStatus.ACTIVE));
        when(limitUsageRepository.upsertLimitUsage(anyString(), any(), any(), any(), any())).thenReturn(1);

        AuthorizationResponse response = service.authorize(request(AMOUNT), REQUEST_TIME);

        assertEquals(AuthorizationResponse.STATUS_APPROVED, response.status());
        assertFalse(response.rrn().isBlank());
        assertFalse(response.authCode().isBlank());
        verify(cardManagementClient).reserve(eq(AMOUNT), anyString(), eq(PAN));
        verify(eventNotifier).notify(any());
    }

    @ParameterizedTest(name = "card status {0} must be declined")
    @EnumSource(value = CardModelStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
    void shouldDeclineWhenCardStatusIsNotActive(CardModelStatus status) {
        when(cardManagementClient.getCard(PAN)).thenReturn(card(status));

        AuthorizationResponse response = service.authorize(request(AMOUNT), REQUEST_TIME);

        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        verifyNoInteractions(limitUsageRepository);
        verify(cardManagementClient, never()).reserve(any(), anyString(), anyString());
    }

    @Test
    void shouldDeclineWhenCardWasNotFound() {
        when(cardManagementClient.getCard(PAN)).thenThrow(new CardNotFoundException(PAN));

        AuthorizationResponse response = service.authorize(request(AMOUNT), REQUEST_TIME);

        assertEquals(AuthorizationResponse.CODE_CARD_NOT_FOUND, response.responseCode());
        assertEquals("CARD_NOT_FOUND", response.declineReason());
        verifyNoInteractions(binLookupClient, limitUsageRepository);
    }

    @Test
    void shouldDeclineWhenCardIsExpiredAtTransactionDate() {
        when(cardManagementClient.getCard(PAN)).thenReturn(card(
                CardModelStatus.ACTIVE, YearMonth.of(2026, 10), new BigDecimal("10000")));

        AuthorizationResponse response = service.authorize(request(AMOUNT), REQUEST_TIME);

        assertEquals(AuthorizationResponse.CODE_CARD_EXPIRED, response.responseCode());
        verifyNoInteractions(limitUsageRepository);
    }

    @Test
    void shouldDeclineWhenAmountExceedsAvailableBalance() {
        when(cardManagementClient.getCard(PAN)).thenReturn(card(
                CardModelStatus.ACTIVE, YearMonth.of(2027, 12), new BigDecimal("999.99")));

        AuthorizationResponse response = service.authorize(request(AMOUNT), REQUEST_TIME);

        assertEquals(AuthorizationResponse.CODE_INSUFFICIENT_FUNDS, response.responseCode());
        verifyNoInteractions(limitUsageRepository);
    }

    @Test
    void shouldDeclineWhenDailyOrMonthlyLimitIsExceeded() {
        when(cardManagementClient.getCard(PAN)).thenReturn(card(CardModelStatus.ACTIVE));
        when(limitUsageRepository.upsertLimitUsage(anyString(), any(), any(), any(), any())).thenReturn(0);

        AuthorizationResponse response = service.authorize(request(AMOUNT), REQUEST_TIME);

        assertEquals(AuthorizationResponse.CODE_EXCEEDS_LIMIT, response.responseCode());
        verify(cardManagementClient, never()).reserve(any(), anyString(), anyString());
    }

    @Test
    void shouldReturnDeclinedWhenRollbackConflictsWithAlreadyRolledBackReservation() {
        RollbackRequest request = new RollbackRequest("123456789012", PAN, AMOUNT);
        doThrow(new RollbackConflictException("already rolled back"))
                .when(cardManagementClient).rollback(request);

        RollbackResponse response = service.rollback(request, REQUEST_TIME);

        assertEquals(RollbackResponse.STATUS_DECLINED, response.status());
        assertEquals("ALREADY_ROLLED_BACK", response.declineReason());
        assertNotNull(response.processingTimeMs());
    }

    @Test
    void shouldApproveRollbackWhenCardManagementAcceptsIt() {
        RollbackRequest request = new RollbackRequest("123456789012", PAN, AMOUNT);

        RollbackResponse response = service.rollback(request, REQUEST_TIME);

        assertEquals(RollbackResponse.STATUS_APPROVED, response.status());
        assertFalse(response.rrn().isBlank());
        verify(cardManagementClient).rollback(request);
    }

    private static AuthorizationRequest request(BigDecimal amount) {
        return new AuthorizationRequest("0100", "123456", PAN, "000000", amount, "643",
                TRANSACTION_TIME, "TERM0001", "POS", "MERCH0000000002", "5045", "ACQ001", "ISS001");
    }

    private static CardModel card(CardModelStatus status) {
        return card(status, YearMonth.of(2027, 12), new BigDecimal("10000"));
    }

    private static CardModel card(CardModelStatus status, YearMonth expiry, BigDecimal balance) {
        return new CardModel(UUID.randomUUID(), PAN, "400000", "TEST USER", expiry, status, "643",
                new BigDecimal("5000"), new BigDecimal("50000"), balance, "ISS001", TRANSACTION_TIME);
    }
}
