package com.processing.transactionlogger.dto;

/**
 * Ответ на запрос {@code GET /health}.
 *
 * @param status             статус сервиса (всегда {@code "ok"} если сервис жив)
 * @param service            имя сервиса ({@code "transaction-logger"})
 * @param transactionsStored общее число транзакций, хранящихся в БД
 */
public record HealthResponse(
        String status,
        String service,
        Long transactionsStored
) {
}
