package com.processing.common.dto.transactionlogger;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Короткий ответ transaction-logger после сохранения новой транзакции.
 *
 * @param id идентификатор сохранённой транзакции
 * @param status статус результата сохранения
 */
@Schema(description = "Ответ после сохранения новой транзакции")
public record TransactionStoredResponse(
        @Schema(description = "Идентификатор сохраненной транзакции")
        UUID id,
        @Schema(description = "Статус результата сохранения")
        String status
) {
}
