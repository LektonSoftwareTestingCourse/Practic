package com.processing.cardmanagement.models;

import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import com.processing.cardmanagement.exceptions.RollbackAlreadySatisfiedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardDomainTest {
    private static final String PAN = "4000001234567890";
    private static final BigDecimal BALANCE = new BigDecimal("1000.00");

    @Test
    void shouldCreateReservationForActiveCardWithinAvailableBalance() {
        Reservation reservation = card(CardStatus.ACTIVE).startReservation(new BigDecimal("250.00"), "RRN001");

        assertEquals(PAN, reservation.pan());
        assertEquals(new BigDecimal("250.00"), reservation.reservationAmount());
        assertEquals(ReservationStatus.RESERVED, reservation.status());
    }

    @Test
    void shouldRejectReservationForInactiveCard() {
        assertThrows(IllegalStateException.class,
                () -> card(CardStatus.BLOCKED).startReservation(BigDecimal.ONE, "RRN001"));
    }

    @Test
    void shouldRejectReservationWhenBalanceIsInsufficient() {
        assertThrows(InsufficientFundsException.class,
                () -> card(CardStatus.ACTIVE).startReservation(new BigDecimal("1000.01"), "RRN001"));
    }

    @Test
    void shouldDecreaseAvailableBalanceWhenReservationIsApplied() {
        Card updated = card(CardStatus.ACTIVE).withReservation(
                new Reservation(PAN, new BigDecimal("250.00"), "RRN001"));

        assertEquals(new BigDecimal("750.00"), updated.availableBalance());
    }

    @Test
    void shouldRejectReservationForAnotherCard() {
        assertThrows(IllegalArgumentException.class,
                () -> card(CardStatus.ACTIVE).withReservation(
                        new Reservation("4111111111111111", BigDecimal.ONE, "RRN001")));
    }

    @Test
    void shouldIncreaseAvailableBalanceWhenRollbackIsApplied() {
        Card original = card(CardStatus.ACTIVE);
        Reservation reservation = new Reservation(PAN, new BigDecimal("250.00"), "RRN001");
        ReservationRollback rollback = reservation.startRollback(new BigDecimal("250.00"));

        assertEquals(new BigDecimal("1250.00"), original.withRollback(rollback).availableBalance());
    }

    @Test
    void shouldRejectNegativeLimitsAndDailyLimitAboveMonthlyLimit() {
        Card original = card(CardStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class,
                () -> original.withData(CardStatus.ACTIVE, new BigDecimal("-1"), new BigDecimal("100"), BALANCE));
        assertThrows(IllegalArgumentException.class,
                () -> original.withData(CardStatus.ACTIVE, new BigDecimal("101"), new BigDecimal("100"), BALANCE));
    }

    @Test
    void shouldAllowDailyLimitEqualToMonthlyLimit() {
        Card updated = card(CardStatus.ACTIVE).withData(
                CardStatus.ACTIVE, new BigDecimal("100"), new BigDecimal("100"), BALANCE);

        assertEquals(new BigDecimal("100"), updated.dailyLimit());
    }

    @Test
    void shouldNotDeleteAlreadyDeletedCardTwice() {
        Card deleted = card(CardStatus.ACTIVE).deleted();
        assertEquals(CardStatus.DELETED, deleted.status());
        assertThrows(IllegalStateException.class, deleted::deleted);
    }

    @Test
    void shouldNotStartRollbackForAlreadyRolledBackReservation() {
        Reservation reservation = new Reservation(PAN, BigDecimal.TEN, "RRN001");
        ReservationRollback rollback = reservation.startRollback(BigDecimal.TEN);
        Reservation rolledBack = reservation.rolledBack(rollback);

        assertEquals(ReservationStatus.ROLLED_BACK, rolledBack.status());
        assertThrows(RollbackAlreadySatisfiedException.class,
                () -> rolledBack.startRollback(BigDecimal.TEN));
    }

    private static Card card(CardStatus status) {
        return new Card(UUID.randomUUID(), PAN, "400000", "TEST USER", YearMonth.of(2028, 12), status,
                "643", new BigDecimal("500.00"), new BigDecimal("2000.00"), BALANCE, "ISS001", Instant.EPOCH);
    }
}
