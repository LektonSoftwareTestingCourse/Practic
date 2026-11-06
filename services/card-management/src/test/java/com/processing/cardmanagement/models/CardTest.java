package com.processing.cardmanagement.models;

import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardTest {

    private static final String PAN = "4000000000000002";
    private static final String RRN = "123456789012";

    @Test
    void shouldCreateReservationWhenBalanceIsEnough() {
        Card card = card(new BigDecimal("1000"));

        Reservation reservation = card.startReservation(new BigDecimal("300"), RRN);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.reservationAmount()).isEqualByComparingTo("300");
    }

    @Test
    void shouldDecreaseAvailableBalanceWhenReservationIsApplied() {
        Card card = card(new BigDecimal("1000"));
        Reservation reservation = card.startReservation(new BigDecimal("300"), RRN);

        Card updated = card.withReservation(reservation);

        assertThat(updated.availableBalance()).isEqualByComparingTo("700");
    }

    @Test
    void shouldThrowWhenBalanceIsNotEnough() {
        Card card = card(new BigDecimal("1000"));

        assertThatThrownBy(() -> card.startReservation(new BigDecimal("1001"), RRN))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Not enough funds on this account");
    }

    @Test
    void shouldMarkCardAsDeletedWhenCardIsActive() {
        Card card = card(new BigDecimal("1000"));

        Card deleted = card.deleted();

        assertThat(deleted.status()).isEqualTo(CardStatus.DELETED);
        assertThat(deleted.pan()).isEqualTo(card.pan());
    }

    @Test
    void shouldRejectDeleteWhenCardIsAlreadyDeleted() {
        Card deleted = card(new BigDecimal("1000")).deleted();

        assertThatThrownBy(deleted::deleted)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Card already deleted");
    }

    @ParameterizedTest
    @EnumSource(CardStatus.class)
    void shouldSetStatusWhenCardDataIsUpdated(CardStatus status) {
        Card card = card(new BigDecimal("1000"));

        Card updated = card.withData(
                status,
                new BigDecimal("150000"),
                new BigDecimal("300000"),
                new BigDecimal("1000")
        );

        assertThat(updated.status()).isEqualTo(status);
    }

    private static Card card(BigDecimal balance) {
        return new Card(
                UUID.randomUUID(),
                PAN,
                "400000",
                "IVAN IVANOV",
                YearMonth.of(2030, 1),
                CardStatus.ACTIVE,
                "643",
                new BigDecimal("150000"),
                new BigDecimal("300000"),
                balance,
                "ISS001"
        );
    }
}
