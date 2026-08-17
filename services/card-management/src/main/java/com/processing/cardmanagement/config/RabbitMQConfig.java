package com.processing.cardmanagement.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology configuration for Card Management → Notification Service.
 *
 * <p>Declares exchange, queue, DLX/DLQ, and bindings for card events
 * published via the outbox processor. The queue uses a topic exchange
 * with routing key pattern {@code card.*}.</p>
 */
@Configuration
public class RabbitMQConfig {

    /** Topic exchange for card events. */
    public static final String CARD_EVENTS_EXCHANGE = "smp.card-events";

    /** Routing key prefix for card events (producer uses "card." + event class simple name). */
    public static final String ROUTING_KEY_PREFIX = "card.";

    /** Durable queue consumed by Notification Service. */
    public static final String QUEUE = "card-notifications";

    /** Dead-letter exchange (direct). */
    public static final String DLX_EXCHANGE = "smp.card-events.dlx";

    /** Dead-letter queue for diagnostic purposes. */
    public static final String DLQ_QUEUE = "card-notifications-dlq";

    /** Routing key used to bind DLX → DLQ. */
    public static final String DLQ_ROUTING_KEY = "card-notifications";

    /** TTL for dead-letter queue messages (60 seconds). */
    private static final int DLQ_TTL_MS = 60_000;

    /** Topic exchange for card events. */
    @Bean
    public TopicExchange cardEventsExchange() {
        return new TopicExchange(CARD_EVENTS_EXCHANGE, true, false);
    }

    /**
     * Durable main queue with dead-letter configuration.
     *
     * <p>After 3 delivery attempts (x-max-delivery) the message
     * goes to DLX {@value #DLX_EXCHANGE}.</p>
     */
    @Bean
    public Queue cardNotificationsQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-max-delivery", 3)
                .build();
    }

    /** Binding: main exchange → main queue with pattern {@code card.*}. */
    @Bean
    public Binding cardNotificationsBinding(
            TopicExchange cardEventsExchange,
            Queue cardNotificationsQueue) {
        return BindingBuilder.bind(cardNotificationsQueue)
                .to(cardEventsExchange)
                .with(ROUTING_KEY_PREFIX + "*");
    }

    /** Dead-letter exchange (direct). */
    @Bean
    public DirectExchange cardEventsDlx() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    /**
     * Dead-letter queue with {@value #DLQ_TTL_MS}ms TTL for diagnostics.
     */
    @Bean
    public Queue cardNotificationsDlq() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .ttl(DLQ_TTL_MS)
                .build();
    }

    /** Binding: DLX → DLQ with routing key. */
    @Bean
    public Binding cardDlqBinding(
            DirectExchange cardEventsDlx,
            Queue cardNotificationsDlq) {
        return BindingBuilder.bind(cardNotificationsDlq)
                .to(cardEventsDlx)
                .with(DLQ_ROUTING_KEY);
    }

    /**
     * Configures Jackson-based JSON message converter for {@link RabbitTemplate}.
     *
     * <p>Ensures that {@link com.processing.cardmanagement.events.CardEvent}
     * instances are serialized to JSON with type information.</p>
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Pre-configured {@link RabbitTemplate} with JSON converter and
     * publisher confirmations.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter);
        return template;
    }
}
