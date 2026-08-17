package com.processing.cardmanagement.models;

import com.processing.cardmanagement.exceptions.RollbackAlreadySatisfiedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Информация об удержании средств
 *
 * @param id                идентификатор
 * @param pan               номер карты
 * @param reservationAmount количество удерживаемых средств
 * @param rrn               RRN-номер
 * @param status            статус
 * @param createdAt         дата создания
 * @param updatedAt         дата изменения
 */
public record Reservation(
    UUID id,
    String pan,
    BigDecimal reservationAmount,
    String rrn,
    ReservationStatus status,
    Instant createdAt,
    Instant updatedAt
) {

    public Reservation(String pan, BigDecimal amount, String rrn) {
        this(
            UUID.randomUUID(),
            pan,
            amount,
            rrn,
            ReservationStatus.RESERVED,
            Instant.now(),
            Instant.now()
        );
    }

    /**
     * Создает возврат средств за удержание
     *
     * @param rollbackAmount количество возвращаемых средств
     * @return измененную сущность
     */
    public ReservationRollback startRollback(BigDecimal rollbackAmount) {
        if (status == ReservationStatus.ROLLED_BACK) {
            throw new RollbackAlreadySatisfiedException(rrn);
        }

        return new ReservationRollback(
            UUID.randomUUID(),
            this.id,
            pan,
            rollbackAmount,
            rrn,
            createdAt
        );
    }

    /**
     * Помечает, что средства за удержание были возвращены
     *
     * @param rollback роллбек удержания
     * @return сущность с обновленными данными
     */
    public Reservation rolledBack(ReservationRollback rollback) {
        if (!Objects.equals(this.id, rollback.reservationId())) {
            throw new IllegalArgumentException("Rollback's reservation ID and this ID differ");
        }
        return new Reservation(
            id,
            pan,
            reservationAmount,
            rrn,
            ReservationStatus.ROLLED_BACK,
            createdAt,
            Instant.now()
        );
    }
}
