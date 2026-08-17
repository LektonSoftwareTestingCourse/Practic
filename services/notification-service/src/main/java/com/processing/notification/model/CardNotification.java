package com.processing.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a received card notification.
 *
 * <p>Stored in {@code card_notifications} table with raw JSON payload
 * to avoid coupling to Card Management module types.</p>
 */
@Entity
@Table(name = "card_notifications")
@Getter
@Setter
@NoArgsConstructor
public class CardNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Simple class name of the event, e.g. "CardServiceCreationEvent". */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** Full routing key, e.g. "card.CardServiceCreationEvent". */
    @Column(name = "routing_key", nullable = false, length = 100)
    private String routingKey;

    /** Serialized event as JSON string. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private String payload;

    /** Timestamp when the notification was received. */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /**
     * Constructs a notification with the current timestamp.
     *
     * @param eventType  simple class name of the event
     * @param routingKey full routing key received from RabbitMQ
     * @param payload    serialized event JSON
     */
    public CardNotification(String eventType, String routingKey, String payload) {
        this.eventType = eventType;
        this.routingKey = routingKey;
        this.payload = payload;
        this.receivedAt = Instant.now();
    }
}
