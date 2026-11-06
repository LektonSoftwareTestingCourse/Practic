package com.processing.cardmanagement.models;

import com.processing.cardmanagement.exceptions.RollbackAlreadySatisfiedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private static final String PAN = "4000000000000002";
    private static final String RRN = "123456789012";

    @Test
    void shouldCreateRollbackWhenReservationIsActive() {
        Reservation reservation = reservation();

        ReservationRollback rollback = reservation.startRollback(new BigDecimal("300"));

        assertThat(rollback.reservationId()).isEqualTo(reservation.id());
        assertThat(rollback.pan()).isEqualTo(PAN);
        assertThat(rollback.rrn()).isEqualTo(RRN);
        assertThat(rollback.rollbackAmount()).isEqualByComparingTo("300");
    }

    @Test
    void shouldMarkReservationAsRolledBackWhenRollbackIsApplied() {
        Reservation reservation = reservation();
        ReservationRollback rollback = reservation.startRollback(new BigDecimal("300"));

        Reservation rolledBack = reservation.rolledBack(rollback);

        assertThat(rolledBack.id()).isEqualTo(reservation.id());
        assertThat(rolledBack.status()).isEqualTo(ReservationStatus.ROLLED_BACK);
    }

    @Test
    void shouldRejectRollbackWhenReservationIsAlreadyRolledBack() {
        Reservation reservation = reservation();
        Reservation rolledBack = reservation.rolledBack(
                reservation.startRollback(new BigDecimal("300")));

        assertThatThrownBy(() -> rolledBack.startRollback(new BigDecimal("300")))
                .isInstanceOf(RollbackAlreadySatisfiedException.class)
                .hasMessageContaining(RRN);
    }

    @Test
    void shouldRejectRollbackWhenReservationIdDiffers() {
        Reservation reservation = reservation();
        ReservationRollback rollback = new ReservationRollback(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PAN,
                new BigDecimal("300"),
                RRN,
                Instant.parse("2026-01-01T10:00:00Z")
        );

        assertThatThrownBy(() -> reservation.rolledBack(rollback))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rollback's reservation ID and this ID differ");
    }

    private static Reservation reservation() {
        return new Reservation(PAN, new BigDecimal("300"), RRN);
    }
}
