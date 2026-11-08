package com.processing.cardmanagement.models;

import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardTest {

    private static final String PAN = "4000001234567890";

    @Test
    void shouldReserveAmountEqualToAvailableBalance() {
        // Arrange
        Card card = card(CardStatus.ACTIVE, new BigDecimal("1000"));

        // Act
        Reservation reservation = card.startReservation(new BigDecimal("1000"), "012345678901");
        Card updatedCard = card.withReservation(reservation);

        // Assert
        assertEquals(ReservationStatus.RESERVED, reservation.status());
        assertEquals(0, updatedCard.availableBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldRejectReservationWhenFundsAreInsufficient() {
        // Arrange
        Card card = card(CardStatus.ACTIVE, new BigDecimal("999.99"));

        // Act and Assert
        assertThrows(InsufficientFundsException.class,
                () -> card.startReservation(new BigDecimal("1000"), "012345678901"));
    }

    @ParameterizedTest(name = "status={0}")
    @EnumSource(value = CardStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
    void shouldRejectReservationForNonActiveCard(CardStatus status) {
        // Arrange
        Card card = card(status, new BigDecimal("1000"));

        // Act and Assert
        assertThrows(IllegalStateException.class,
                () -> card.startReservation(new BigDecimal("1"), "012345678901"));
    }

    @Test
    void shouldRestoreBalanceAfterRollback() {
        // Arrange
        Card card = card(CardStatus.ACTIVE, new BigDecimal("1000"));
        Reservation reservation = card.startReservation(new BigDecimal("400"), "012345678901");
        Card reservedCard = card.withReservation(reservation);
        ReservationRollback rollback = reservation.startRollback(new BigDecimal("400"));

        // Act
        Card restoredCard = reservedCard.withRollback(rollback);

        // Assert
        assertEquals(0, restoredCard.availableBalance().compareTo(new BigDecimal("1000")));
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 1000",
            "100, -1",
            "1001, 1000"
    })
    void shouldRejectInvalidLimits(String dailyLimit, String monthlyLimit) {
        // Arrange
        Card card = card(CardStatus.ACTIVE, new BigDecimal("1000"));

        // Act and Assert
        assertThrows(IllegalArgumentException.class, () -> card.withData(
                CardStatus.ACTIVE, new BigDecimal(dailyLimit), new BigDecimal(monthlyLimit),
                card.availableBalance()));
    }

    @Test
    void shouldRejectReservationCreatedForAnotherCard() {
        // Arrange
        Card card = card(CardStatus.ACTIVE, new BigDecimal("1000"));
        Reservation foreignReservation = new Reservation("5000001234567890", BigDecimal.ONE, "012345678901");

        // Act and Assert
        assertThrows(IllegalArgumentException.class, () -> card.withReservation(foreignReservation));
    }

    private Card card(CardStatus status, BigDecimal balance) {
        return new Card(
                UUID.randomUUID(), PAN, "400000", "IRINA EREMINA", YearMonth.now().plusYears(1), status,
                "643", new BigDecimal("10000"), new BigDecimal("100000"), balance,
                "ISS001", Instant.parse("2026-01-01T00:00:00Z"));
    }
}
