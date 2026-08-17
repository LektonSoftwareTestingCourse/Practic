package com.processing.notification.listener;

import com.processing.notification.config.RabbitMQConfig;
import com.processing.notification.model.CardNotification;
import com.processing.notification.repository.CardNotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes card events from RabbitMQ queue {@code card-notifications}.
 *
 * <p>Receives events as raw JSON strings to avoid coupling to Card Management
 * module types. Extracts the event type from the routing key
 * (everything after {@code "card."}) and persists the notification.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CardEventListener {

    private final CardNotificationRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Handles a card event message from RabbitMQ.
     *
     * @param message    raw JSON payload as received from the exchange
     * @param routingKey full routing key, e.g. {@code "card.CardServiceCreationEvent"}
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    @Transactional
    public void handleCardEvent(
            String message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        try {
            String eventType = extractEventType(routingKey);
            // Parse to validate JSON, then re-serialize for consistent storage
            Object parsed = objectMapper.readValue(message, Object.class);
            String payload = objectMapper.writeValueAsString(parsed);

            CardNotification notification = new CardNotification(eventType, routingKey, payload);
            repository.save(notification);
            log.info("Received card event: type={}, routingKey={}", eventType, routingKey);
        } catch (Exception e) {
            log.error("Failed to process card event: routingKey={}, error={}", routingKey, e.getMessage());
            throw new RuntimeException("Failed to process card event", e);
        }
    }

    /**
     * Extracts the event type from the routing key.
     *
     * <p>For routing key {@code "card.CardServiceCreationEvent"},
     * returns {@code "CardServiceCreationEvent"}.</p>
     *
     * @param routingKey full routing key
     * @return event type substring after the prefix
     */
    private String extractEventType(String routingKey) {
        if (routingKey != null && routingKey.startsWith(RabbitMQConfig.ROUTING_KEY_PREFIX)) {
            return routingKey.substring(RabbitMQConfig.ROUTING_KEY_PREFIX.length());
        }
        return routingKey != null ? routingKey : "unknown";
    }
}
