package com.processing.merchantacquirer.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Запрос на получение рассчитанной комиссии эквайрера по транзакции")
public record AcquirerFeeRequest(
        @Schema(description = "Дата и время передачи транзакции (ISO-8601)",
            example = "2026-06-22T10:15:30Z", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Instant transmissionDateTime,

        @Schema(description = "Номер карты (PAN)", example = "4000000000000002",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String pan,

        @Schema(description = "System trace audit number (STAN)", example = "000301",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String stan,

        @Schema(description = "Сумма транзакции в копейках", example = "85000")
        BigDecimal amount,

        @Schema(description = "Идентификатор терминала", example = "TERM042",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String terminalId
){
}
