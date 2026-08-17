package com.processing.service;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Клиент для асинхронной отправки транзакций в Transaction Logger через RabbitMQ.
 *
 * <p>Публикует {@link TransactionRequest} в durable очередь {@code transaction-log}
 * с publisher confirms. При недоступности брокера возвращает {@code false},
 * что позволяет {@code RouteService} выполнить rollback для APPROVED-транзакций.</p>
 */
@Service
public class LoggerClient {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerClient.class);

    private final RabbitTemplate rabbitTemplate;

    /**
     * @param rabbitTemplate настроенный {@link RabbitTemplate} с publisher confirms
     */
    public LoggerClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Публикует транзакцию в очередь {@code transaction-log} через exchange {@code smp.transactions}.
     *
     * @param transaction DTO транзакции для сохранения в Logger
     * @return {@code true} если брокер принял сообщение, {@code false} при {@link AmqpException}
     */
    public boolean log(TransactionRequest transaction) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    transaction
            );
            LOG.info("Published TX {} to queue", transaction.stan());
            return true;
        } catch (AmqpException e) {
            LOG.error("Failed to publish TX {} to queue: {}", transaction.stan(), e.getMessage());
            return false;
        }
    }
}
