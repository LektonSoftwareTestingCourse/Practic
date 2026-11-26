package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.exceptions.TooLargeLimitException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.ReservationRepository;
import com.processing.cardmanagement.repositories.ReservationRollbackRepository;
import com.processing.cardmanagement.services.retries.RetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CardServiceImplTest {
    private static final String PAN = "4000001234567890";
    private CardRepository cardRepository;
    private ReservationRepository reservationRepository;
    private CardServiceImpl service;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        ReservationRollbackRepository rollbackRepository = mock(ReservationRollbackRepository.class);
        CardServiceSettings settings = mock(CardServiceSettings.class);
        CardServiceDefaults defaults = mock(CardServiceDefaults.class);
        PanGenerator panGenerator = mock(PanGenerator.class);
        CardEventNotifier notifier = mock(CardEventNotifier.class);
        BinIssuerService binIssuerService = mock(BinIssuerService.class);
        RetryService retryService = mock(RetryService.class);
        TransactionRunner directTransactionRunner = new TransactionRunner() {
            @Override public void run(Runnable operation) { operation.run(); }
            @Override public <T> T runSupplier(java.util.function.Supplier<T> operation) { return operation.get(); }
            @Override public <T> T runSupplierWithNestedQuery(
                    java.util.function.Function<TransactionRunner, T> supplier) { return supplier.apply(this); }
        };
        when(settings.maxPageLimit()).thenReturn(100);
        when(defaults.pageLimit()).thenReturn(20);
        when(defaults.pageOffset()).thenReturn(0L);
        service = new CardServiceImpl(cardRepository, reservationRepository, rollbackRepository, settings,
                defaults, panGenerator, notifier, binIssuerService, retryService, directTransactionRunner);
    }

    @Test
    void shouldReturnCardWhenPanExists() {
        Card card = card();
        when(cardRepository.findByPan(PAN)).thenReturn(Optional.of(card));

        assertEquals(card, service.getCard(PAN));
    }

    @Test
    void shouldThrowWhenPanDoesNotExist() {
        when(cardRepository.findByPan(PAN)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> service.getCard(PAN));
    }

    @Test
    void shouldApplyDefaultPaginationValuesWhenArgumentsAreMissing() {
        when(cardRepository.findCards(20, 0L, null, null, null, null, null)).thenReturn(List.of(card()));

        assertEquals(1, service.getCards(null, null, null, null, null, null, null).size());
    }

    @Test
    void shouldRejectPageLimitAboveConfiguredMaximum() {
        assertThrows(TooLargeLimitException.class,
                () -> service.getCards(101, 0L, null, null, null, null, null));
        verifyNoInteractions(cardRepository);
    }

    @Test
    void shouldReserveFundsAndPersistUpdatedBalance() {
        Card original = card();
        when(cardRepository.findByPanForUpdate(PAN)).thenReturn(Optional.of(original));
        when(reservationRepository.isUnique("RRN001", PAN)).thenReturn(true);
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Card updated = service.reserve(PAN, new BigDecimal("250.00"), "RRN001");

        assertEquals(new BigDecimal("750.00"), updated.availableBalance());
        verify(reservationRepository).save(any());
        verify(cardRepository).update(any());
    }

    private static Card card() {
        return new Card(UUID.randomUUID(), PAN, "400000", "TEST USER", YearMonth.of(2028, 12),
                CardStatus.ACTIVE, "643", new BigDecimal("500"), new BigDecimal("2000"),
                new BigDecimal("1000.00"), "ISS001", Instant.EPOCH);
    }
}
