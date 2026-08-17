package com.processing.transactionlogger.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology configuration for Transaction Logger.
 *
 * <p>Mirrors the Switch-side configuration to declare the same
 * exchange, queue, and bindings for idempotent setup.</p>
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "smp.transactions";
    public static final String ROUTING_KEY = "transaction.log";
    public static final String QUEUE = "transaction-log";

    public static final String DLX_EXCHANGE = "smp.transactions.dlx";
    public static final String DLQ_QUEUE = "transaction-log-dlq";
    public static final String DLQ_ROUTING_KEY = "transaction-log";

    @Bean
    public TopicExchange smpTransactionsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * Durable main queue with dead-letter configuration.
     *
     * <p>After 3 delivery attempts (x-max-delivery) the message
     * goes to DLX {@value #DLX_EXCHANGE}. Mirrors the Switch-side
     * declaration for idempotent topology setup.</p>
     */
    @Bean
    public Queue transactionLogQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-max-delivery", 3)
                .build();
    }

    @Bean
    public Binding transactionLogBinding(
            TopicExchange smpTransactionsExchange,
            Queue transactionLogQueue) {
        return BindingBuilder.bind(transactionLogQueue)
                .to(smpTransactionsExchange)
                .with(ROUTING_KEY);
    }
}
