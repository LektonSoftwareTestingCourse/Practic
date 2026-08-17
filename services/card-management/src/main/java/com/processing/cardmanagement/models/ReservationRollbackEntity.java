package com.processing.cardmanagement.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "reservation_rollbacks")
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRollbackEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "reservation_id",
        referencedColumnName = "id",
        unique = true
    )
    private ReservationEntity reservation;

    @Column(length = 16, nullable = false)
    private String pan;

    @Column(nullable = false, precision = 19)
    private BigDecimal rollbackAmount;

    @Column(length = 12, nullable = false)
    private String rrn;

    private Instant createdAt;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !Objects.equals(this.getClass(), obj.getClass())) {
            return false;
        }
        var other = (ReservationRollbackEntity) obj;
        return Objects.equals(this.id, other.id)
            && Objects.equals(
            this.reservation != null ? this.reservation.getId() : null,
            other.getReservation() != null ? other.getReservation().getId() : null)
            && Objects.equals(this.pan, other.pan)
            && ((this.rollbackAmount == null && other.rollbackAmount == null)
            || (this.rollbackAmount != null && this.rollbackAmount.compareTo(
            other.rollbackAmount.setScale(0, RoundingMode.HALF_EVEN)) == 0))
            && Objects.equals(this.rrn, other.rrn)
            && Objects.equals(this.createdAt, other.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            reservation == null ? null : reservation.getId(),
            pan,
            rollbackAmount == null ? null : rollbackAmount.setScale(0, RoundingMode.HALF_EVEN),
            rrn,
            createdAt
        );
    }
}
