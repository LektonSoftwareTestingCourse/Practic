package com.processing.transactionlogger.listener;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.transactionlogger.config.RabbitMQConfig;
import com.processing.transactionlogger.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link TransactionRequest} messages from the {@code transaction-log} queue
 * and delegates to {@link TransactionService#store(TransactionRequest)}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionLogListener {

    private final TransactionService transactionService;

    /**
     * Handles a transaction received from RabbitMQ.
     *
     * @param request transaction data from Switch
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleTransaction(TransactionRequest request) {
        log.info("Received TX {} from queue", request.stan());
        transactionService.store(request);
    }
}
