package com.processing.common.dto.transactionlogger;

import com.processing.common.dto.annotations.Pan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Запрос на сохранение транзакции в transaction-logger.
 *
 * @param id идентификатор транзакции, сгенерированный Switch
 * @param mti тип ISO 8583 сообщения
 * @param stan системный трассировочный номер операции
 * @param rrn ссылочный номер транзакции, сгенерированный сервисом авторизации
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
 * @param acquiringFee комиссия эквайринга
 * @param status статус результата авторизации
 * @param declineReason причина отказа для отклонённой транзакции
 * @param authCode код авторизации
 * @param processingTimeMs время обработки транзакции в миллисекундах
 * @param transmissionDateTime дата и время передачи транзакции
 * @param createdAt дата и время создания транзакции, переданные Switch
 */
@Schema(description = "Данные транзакции, полученные от Switch")
public record TransactionRequest(
        @Schema(description = "Идентификатор транзакции, сгенерированный Switch")
        @NotNull
        UUID id,

        @Schema(description = "Тип ISO 8583 сообщения")
        @NotBlank
        @Size(max = 4)
        String mti,

        @Schema(description = "Системный трассировочный номер операции")
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "stan must be exactly 6 digits")
        String stan,

        @Schema(description = "Ссылочный номер транзакции (RRN), сгенерированный сервисом авторизации")
        @Size(max = 12)
        String rrn,

        @Schema(description = "Номер тестовой или замаскированной карты (PAN)")
        @NotBlank
        @Pan
        String pan,

        @Schema(description = "Код обработки транзакции")
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "processingCode must be exactly 6 digits")
        String processingCode,

        @Schema(description = "Сумма транзакции в минимальных единицах валюты")
        @NotNull
        @Positive
        BigDecimal amount,

        @Schema(description = "Числовой код валюты по ISO 4217")
        @NotBlank
        @Pattern(regexp = "\\d{3}", message = "currencyCode must be exactly 3 digits")
        String currencyCode,

        @Schema(description = "Идентификатор терминала")
        @NotBlank
        @Size(max = 8)
        String terminalId,

        @Schema(description = "Тип терминала")
        @Size(max = 10)
        String terminalType,

        @Schema(description = "Идентификатор мерчанта")
        @NotBlank
        @Size(max = 15)
        String merchantId,

        @Schema(description = "Код категории мерчанта")
        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "mcc must be exactly 4 digits")
        String mcc,

        @Schema(description = "Идентификатор эквайера")
        @NotBlank
        @Size(max = 10)
        String acquirerId,

        @Schema(description = "Идентификатор эмитента")
        @Size(max = 10)
        String issuerId,

        @Schema(description = "Комиссия эквайринга")
        @PositiveOrZero
        BigDecimal acquiringFee,

        @Schema(description = "Статус результата авторизации")
        @NotNull
        TransactionStatus status,

        @Schema(description = "Причина отказа для отклоненной транзакции")
        @Size(max = 100)
        String declineReason,

        @Schema(description = "Код авторизации")
        @Size(max = 6)
        String authCode,

        @Schema(description = "Время обработки транзакции в миллисекундах")
        @PositiveOrZero
        Integer processingTimeMs,

        @Schema(description = "Дата и время передачи транзакции")
        @NotNull
        Instant transmissionDateTime,

        @Schema(description = "Дата и время создания транзакции, переданные Switch")
        @NotNull
        Instant createdAt
) implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
}
