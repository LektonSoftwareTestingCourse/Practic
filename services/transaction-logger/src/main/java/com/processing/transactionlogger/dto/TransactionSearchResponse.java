package com.processing.transactionlogger.dto;

import com.processing.common.dto.transactionlogger.TransactionResponse;

import java.util.List;

/**
 * Ответ на запрос {@code GET /api/transactions/search}.
 *
 * @param total        общее число транзакций, удовлетворяющих фильтру (без учёта пагинации)
 * @param transactions транзакции текущей страницы
 */
public record TransactionSearchResponse(
        long total,
        List<TransactionResponse> transactions
) {
}
