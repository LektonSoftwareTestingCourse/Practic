package com.processing.cardmanagement.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "cards", indexes = {
    @Index(name = "uk_cards_pan", columnList = "pan", unique = true),
    @Index(name = "idx_cards_issuer_id_created_at", columnList = "issuer_id, created_at"),
    @Index(name = "idx_cards_created_at", columnList = "created_at")
})
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("status <> 'DELETED'")
public class CardEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(length = 16, unique = true, nullable = false)
    private String pan;

    @Column(length = 6, nullable = false)
    private String bin;

    @Column(nullable = false)
    private String cardholderName;

    @Column(nullable = false, length = 4)
    private String expiryDate;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(length = 3, nullable = false)
    private String currencyCode;

    @Column(nullable = false, precision = 19)
    private BigDecimal dailyLimit;

    @Column(nullable = false, precision = 19)
    private BigDecimal monthlyLimit;

    @Column(nullable = false, precision = 19)
    private BigDecimal availableBalance;

    @Column(length = 10, nullable = false)
    private String issuerId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
