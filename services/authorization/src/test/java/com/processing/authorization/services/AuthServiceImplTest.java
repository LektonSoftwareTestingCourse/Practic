package com.processing.authorization.services;

import com.processing.authorization.client.BinLookupClient;
import com.processing.authorization.client.CardManagementClient;
import com.processing.authorization.constants.DeclineOutcome;
import com.processing.authorization.events.AuthorizationEvent;
import com.processing.authorization.events.AuthorizationEventNotifier;
import com.processing.authorization.exceptions.CardNotFoundException;
import com.processing.authorization.repositories.LimitUsageRepository;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String PAN = "4000001234567890";
    private static final BigDecimal AMOUNT = new BigDecimal("1000");
    private static final Instant REQUEST_TIME = Instant.now();

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
    void shouldApproveAuthorizationWhenCardIsActiveAndFundsAndLimitsAreSufficient() {
        // Arrange
        AuthorizationRequest request = validRequest();
        when(cardManagementClient.getCard(PAN)).thenReturn(card(CardModelStatus.ACTIVE, new BigDecimal("5000")));
        when(binLookupClient.getIssuerId(PAN)).thenReturn(Optional.of("ISS001"));
        when(limitUsageRepository.upsertLimitUsage(anyString(), any(), any(), any(), any())).thenReturn(1);
        when(limitUsageRepository.fetchRrnBlock()).thenReturn(42L);

        // Act
        AuthorizationResponse response = authService.authorize(request, REQUEST_TIME);

        // Assert
        assertEquals(AuthorizationResponse.STATUS_APPROVED, response.status());
        assertEquals(AuthorizationResponse.CODE_APPROVED, response.responseCode());
        assertNotNull(response.rrn());
        assertEquals(12, response.rrn().length());
        assertNotNull(response.authCode());
        assertEquals(6, response.authCode().length());
        verify(cardManagementClient).reserve(AMOUNT, response.rrn(), PAN);
        verify(eventNotifier).notify(any(AuthorizationEvent.class));
    }

    @ParameterizedTest(name = "status={0}")
    @EnumSource(value = CardModelStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
    void shouldDeclineAuthorizationWhenCardStatusIsNotActive(CardModelStatus status) {
        // Arrange
        when(cardManagementClient.getCard(PAN)).thenReturn(card(status, new BigDecimal("5000")));
        when(binLookupClient.getIssuerId(PAN)).thenReturn(Optional.empty());

        // Act
        AuthorizationResponse response = authService.authorize(validRequest(), REQUEST_TIME);

        // Assert
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        verify(cardManagementClient, never()).reserve(any(), anyString(), anyString());
    }

    @Test
    void shouldDeclineAuthorizationWhenCardDoesNotExist() {
        // Arrange
        when(cardManagementClient.getCard(PAN)).thenThrow(new CardNotFoundException(PAN));

        // Act
        AuthorizationResponse response = authService.authorize(validRequest(), REQUEST_TIME);

        // Assert
        assertEquals(AuthorizationResponse.CODE_CARD_NOT_FOUND, response.responseCode());
        assertEquals(DeclineOutcome.REASON_CARD_NOT_FOUND, response.declineReason());
        verify(cardManagementClient, never()).reserve(any(), anyString(), anyString());
    }

    @Test
    void shouldDeclineAuthorizationWhenAmountExceedsBalance() {
        // Arrange
        when(cardManagementClient.getCard(PAN)).thenReturn(card(CardModelStatus.ACTIVE, new BigDecimal("999.99")));
        when(binLookupClient.getIssuerId(PAN)).thenReturn(Optional.empty());

        // Act
        AuthorizationResponse response = authService.authorize(validRequest(), REQUEST_TIME);

        // Assert
        assertEquals(AuthorizationResponse.CODE_INSUFFICIENT_FUNDS, response.responseCode());
        assertEquals(DeclineOutcome.REASON_INSUFFICIENT_FUNDS, response.declineReason());
        verify(cardManagementClient, never()).reserve(any(), anyString(), anyString());
    }

    @Test
    void shouldDeclineAuthorizationWhenLimitsAreExceeded() {
        // Arrange
        when(cardManagementClient.getCard(PAN)).thenReturn(card(CardModelStatus.ACTIVE, new BigDecimal("5000")));
        when(binLookupClient.getIssuerId(PAN)).thenReturn(Optional.empty());
        when(limitUsageRepository.upsertLimitUsage(anyString(), any(), any(), any(), any())).thenReturn(0);

        // Act
        AuthorizationResponse response = authService.authorize(validRequest(), REQUEST_TIME);

        // Assert
        assertEquals(AuthorizationResponse.CODE_EXCEEDS_LIMIT, response.responseCode());
        assertEquals(DeclineOutcome.REASON_EXCEEDS_AMOUNT_LIMIT, response.declineReason());
        verify(cardManagementClient, never()).reserve(any(), anyString(), anyString());
    }

    @Test
    void shouldReturnFalseWhenLimitRepositoryUpdatesNoRows() {
        // Arrange
        CardModel card = card(CardModelStatus.ACTIVE, new BigDecimal("5000"));
        when(limitUsageRepository.upsertLimitUsage(anyString(), any(), any(), any(), any())).thenReturn(0);

        // Act
        boolean result = authService.checkAndUpdateLimits(card, AMOUNT, REQUEST_TIME.atZone(java.time.ZoneOffset.UTC).toLocalDate());

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldApproveRollbackWhenCardManagementCompletesRollback() {
        // Arrange
        RollbackRequest request = new RollbackRequest("012345678901", PAN, AMOUNT);

        // Act
        RollbackResponse response = authService.rollback(request, REQUEST_TIME);

        // Assert
        assertEquals(RollbackResponse.STATUS_APPROVED, response.status());
        assertEquals(RollbackResponse.CODE_SUCCESS, response.responseCode());
        verify(cardManagementClient).rollback(request);
    }

    @Test
    void shouldDeclineRollbackWhenReservationDoesNotExist() {
        // Arrange
        RollbackRequest request = new RollbackRequest("012345678901", PAN, AMOUNT);
        doThrow(new CardNotFoundException(PAN)).when(cardManagementClient).rollback(request);

        // Act
        RollbackResponse response = authService.rollback(request, REQUEST_TIME);

        // Assert
        assertEquals(RollbackResponse.STATUS_DECLINED, response.status());
        assertEquals(RollbackResponse.CODE_TRANSACTION_NOT_FOUND, response.responseCode());
        assertTrue(response.processingTimeMs() >= 0);
    }

    private AuthorizationRequest validRequest() {
        return new AuthorizationRequest(
                "0100", "000001", PAN, "000000", AMOUNT, "643", Instant.now(),
                "TERM0001", "POS", "MERCH0000000002", "5045", "ACQ002", "ISS001");
    }

    private CardModel card(CardModelStatus status, BigDecimal balance) {
        return new CardModel(
                UUID.randomUUID(), PAN, "400000", "IRINA EREMINA", YearMonth.now().plusYears(1),
                status, "643", new BigDecimal("10000"), new BigDecimal("100000"), balance,
                "ISS001", Instant.now());
    }
}
