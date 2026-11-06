package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.models.Reservation;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.ReservationRepository;
import com.processing.cardmanagement.repositories.ReservationRollbackRepository;
import com.processing.cardmanagement.services.retries.RetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    private static final String BIN = "400000";
    private static final String PAN = "4000000000000002";
    private static final String ISSUER_ID = "ISS001";
    private static final String RRN = "123456789012";

    @Mock
    private CardRepository cardRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationRollbackRepository reservationRollbackRepository;
    @Mock
    private CardServiceSettings settings;
    @Mock
    private CardServiceDefaults defaults;
    @Mock
    private PanGenerator panGenerator;
    @Mock
    private CardEventNotifier eventNotifier;
    @Mock
    private BinIssuerService binIssuerService;
    @Mock
    private RetryService retryService;
    @Mock
    private TransactionRunner transactionRunner;

    private CardServiceImpl cardService;

    @BeforeEach
    void setUp() {
        cardService = new CardServiceImpl(
                cardRepository,
                reservationRepository,
                reservationRollbackRepository,
                settings,
                defaults,
                panGenerator,
                eventNotifier,
                binIssuerService,
                retryService,
                transactionRunner
        );
    }

    @Test
    void shouldCreateActiveCardWhenRequestIsValid() {
        when(binIssuerService.getIssuerId(BIN)).thenReturn(ISSUER_ID);
        when(settings.cardValidityPeriod()).thenReturn(3);
        when(settings.maxCardCreationRetries()).thenReturn(3);
        when(panGenerator.generatePan(BIN)).thenReturn(PAN);
        runRetriesImmediately();
        when(cardRepository.create(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card card = cardService.createCard(
                BIN,
                "IVAN IVANOV",
                "643",
                new BigDecimal("150000"),
                new BigDecimal("300000"),
                new BigDecimal("100000")
        );

        assertThat(card.pan()).hasSize(16).isEqualTo(PAN);
        assertThat(new LuhnValidator().isValid(card.pan())).isTrue();
        assertThat(card.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.expiryDate())
                .isAfter(YearMonth.now().plusYears(2))
                .isBefore(YearMonth.now().plusYears(4));
        assertThat(card.issuerId()).isEqualTo(ISSUER_ID);
        verify(eventNotifier).notifyListeners(any());
    }

    @Test
    void shouldReserveAmountAndDecreaseBalanceWhenCardExists() {
        Card card = card(new BigDecimal("1000"));
        when(cardRepository.findByPanForUpdate(PAN)).thenReturn(Optional.of(card));
        when(reservationRepository.isUnique(RRN, PAN)).thenReturn(true);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardRepository.update(any(Card.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        supplyInTransactionImmediately();

        Card result = cardService.reserve(PAN, new BigDecimal("300"), RRN);

        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(reservationCaptor.capture());

        assertThat(result.availableBalance()).isEqualByComparingTo("700");
        assertThat(reservationCaptor.getValue().reservationAmount()).isEqualByComparingTo("300");
        verify(eventNotifier).notifyListeners(any());
    }

    @Test
    void shouldThrowWhenCardDoesNotExist() {
        when(cardRepository.findByPan(PAN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCard(PAN))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    void shouldDeleteCardWhenCardExists() {
        Card card = card(new BigDecimal("1000"));
        when(cardRepository.findByPanForUpdate(PAN)).thenReturn(Optional.of(card));
        when(cardRepository.update(any(Card.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        runTransactionImmediately();

        cardService.deleteCard(PAN);

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).update(cardCaptor.capture());

        assertThat(cardCaptor.getValue().status()).isEqualTo(CardStatus.DELETED);
        verify(eventNotifier).notifyListeners(any());
    }

    private void runTransactionImmediately() {
        doAnswer(invocation -> {
            Runnable operation = invocation.getArgument(0);
            operation.run();
            return null;
        }).when(transactionRunner).run(any());
    }

    private void supplyInTransactionImmediately() {
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        }).when(transactionRunner).runSupplier(any());
    }

    private void runRetriesImmediately() {
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        }).when(retryService).supply(anyInt(), any());
    }

    private static Card card(BigDecimal balance) {
        return new Card(
                UUID.randomUUID(),
                PAN,
                BIN,
                "IVAN IVANOV",
                YearMonth.of(2030, 1),
                CardStatus.ACTIVE,
                "643",
                new BigDecimal("150000"),
                new BigDecimal("300000"),
                balance,
                ISSUER_ID
        );
    }
}
