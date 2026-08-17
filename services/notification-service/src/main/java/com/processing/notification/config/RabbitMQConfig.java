package com.processing.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology configuration for Notification Service.
 *
 * <p>Mirrors the Card Management side to declare the same
 * exchange, queue, DLX/DLQ, and bindings for idempotent setup.</p>
 */
@Configuration
public class RabbitMQConfig {

    /** Topic exchange for card events. */
    public static final String EXCHANGE = "smp.card-events";

    /** Routing key prefix for card events. */
    public static final String ROUTING_KEY_PREFIX = "card.";

    /** Durable queue consumed by this service. */
    public static final String QUEUE = "card-notifications";

    /** Dead-letter exchange (direct). */
    public static final String DLX_EXCHANGE = "smp.card-events.dlx";

    /** Dead-letter queue for diagnostic purposes. */
    public static final String DLQ_QUEUE = "card-notifications-dlq";

    /** Routing key used to bind DLX → DLQ. */
    public static final String DLQ_ROUTING_KEY = "card-notifications";

    /** TTL for dead-letter queue messages (60 seconds). */
    private static final int DLQ_TTL_MS = 60_000;

    @Bean
    public TopicExchange cardEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue cardNotificationsQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-max-delivery", 3)
                .build();
    }

    @Bean
    public Binding cardNotificationsBinding(
            TopicExchange cardEventsExchange,
            Queue cardNotificationsQueue) {
        return BindingBuilder.bind(cardNotificationsQueue)
                .to(cardEventsExchange)
                .with(ROUTING_KEY_PREFIX + "*");
    }

    @Bean
    public DirectExchange cardEventsDlx() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue cardNotificationsDlq() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .ttl(DLQ_TTL_MS)
                .build();
    }

    @Bean
    public Binding cardDlqBinding(
            DirectExchange cardEventsDlx,
            Queue cardNotificationsDlq) {
        return BindingBuilder.bind(cardNotificationsDlq)
                .to(cardEventsDlx)
                .with(DLQ_ROUTING_KEY);
    }
}
