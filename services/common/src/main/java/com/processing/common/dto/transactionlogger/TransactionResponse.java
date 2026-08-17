package com.processing.common.dto.transactionlogger;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Полное представление сохранённой транзакции, возвращаемое transaction-logger.
 *
 * @param id идентификатор транзакции
 * @param mti тип ISO 8583 сообщения
 * @param stan системный трассировочный номер операции
 * @param rrn ссылочный номер транзакции
 * @param pan номер тестовой или замаскированной карты
 * @param processingCode код обработки транзакции
 * @param amount сумма транзакции в минимальных единицах валюты
 * @param currencyCode числовой код валюты по ISO 4217
 * @param terminalId идентификатор терминала
 * @param terminalType тип терминала
 * @param merchantId идентификатор мерчанта
 * @param mcc код категории мерчанта
 * @param acquirerId идентификатор эквайера
 * @param issuerId идентификатор эмитента
 * @param acquiringFee комиссия эквайринга в минимальных единицах валюты
 * @param status статус результата авторизации
 * @param declineReason причина отказа для отклонённой транзакции
 * @param authCode код авторизации
 * @param processingTimeMs время обработки транзакции в миллисекундах
 * @param transmissionDateTime дата и время передачи транзакции
 * @param createdAt дата и время создания транзакции
 */
@Schema(description = "Полное представление сохраненной транзакции")
public record TransactionResponse(
        @Schema(description = "Идентификатор транзакции")
        UUID id,
        @Schema(description = "Тип ISO 8583 сообщения")
        String mti,
        @Schema(description = "Системный трассировочный номер операции")
        String stan,
        @Schema(description = "Ссылочный номер транзакции (RRN)")
        String rrn,
        @Schema(description = "Номер тестовой или замаскированной карты (PAN)")
        String pan,
        @Schema(description = "Код обработки транзакции")
        String processingCode,
        @Schema(description = "Сумма транзакции в минимальных единицах валюты")
        BigDecimal amount,
        @Schema(description = "Числовой код валюты по ISO 4217")
        String currencyCode,
        @Schema(description = "Идентификатор терминала")
        String terminalId,
        @Schema(description = "Тип терминала")
        String terminalType,
        @Schema(description = "Идентификатор мерчанта")
        String merchantId,
        @Schema(description = "Код категории мерчанта")
        String mcc,
        @Schema(description = "Идентификатор эквайера")
        String acquirerId,
        @Schema(description = "Идентификатор эмитента")
        String issuerId,
        @Schema(description = "Комиссия эквайринга в минимальных единицах валюты")
        BigDecimal acquiringFee,
        @Schema(description = "Статус результата авторизации")
        TransactionStatus status,
        @Schema(description = "Причина отказа для отклоненной транзакции")
        String declineReason,
        @Schema(description = "Код авторизации")
        String authCode,
        @Schema(description = "Время обработки транзакции в миллисекундах")
        Integer processingTimeMs,
        @Schema(description = "Дата и время передачи транзакции")
        Instant transmissionDateTime,
        @Schema(description = "Дата и время создания транзакции")
        Instant createdAt
) {
}
