package com.processing.cardmanagement.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardOutboxEventDataEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    @Column()
    private String payload;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column()
    private Instant processedAt;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column()
    private String lastError;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxEventDataStatus status = OutboxEventDataStatus.PENDING;
}
