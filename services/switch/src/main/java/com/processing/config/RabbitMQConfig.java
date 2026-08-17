package com.processing.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology configuration for Switch → Transaction Logger.
 *
 * <p>Declares exchange, queue, DLX/DLQ, and bindings.
 * Both Switch (producer) and Transaction Logger (consumer) declare
 * the same topology for idempotent setup.</p>
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "smp.transactions";
    public static final String ROUTING_KEY = "transaction.log";
    public static final String QUEUE = "transaction-log";

    public static final String DLX_EXCHANGE = "smp.transactions.dlx";
    public static final String DLQ_QUEUE = "transaction-log-dlq";
    public static final String DLQ_ROUTING_KEY = "transaction-log";

    /** Topic exchange for transaction messages. */
    @Bean
    public TopicExchange smpTransactionsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * Durable main queue with dead-letter configuration.
     *
     * <p>After 3 delivery attempts (x-max-delivery) the message
     * goes to DLX {@value #DLX_EXCHANGE}.</p>
     */
    @Bean
    public Queue transactionLogQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-max-delivery", 3)
                .build();
    }

    /** Binding: main exchange → main queue with routing key. */
    @Bean
    public Binding transactionLogBinding(
            TopicExchange smpTransactionsExchange,
            Queue transactionLogQueue) {
        return BindingBuilder.bind(transactionLogQueue)
                .to(smpTransactionsExchange)
                .with(ROUTING_KEY);
    }

    /** Dead-letter exchange (direct). */
    @Bean
    public DirectExchange smpTransactionsDlx() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    /**
     * Dead-letter queue with 60-second TTL for diagnostics.
     *
     * <p>Messages in DLQ expire after {@value #DLQ_TTL_MS}ms.</p>
     */
    @Bean
    public Queue transactionLogDlq() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .ttl(60_000)
                .build();
    }

    /** Binding: DLX → DLQ with routing key. */
    @Bean
    public Binding dlqBinding(
            DirectExchange smpTransactionsDlx,
            Queue transactionLogDlq) {
        return BindingBuilder.bind(transactionLogDlq)
                .to(smpTransactionsDlx)
                .with(DLQ_ROUTING_KEY);
    }
}
