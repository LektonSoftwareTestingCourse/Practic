package com.processing.common.dto.terminalsimulator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TerminalStartContinuousRequest(
    @NotNull
    @Min(1)
    @Schema(description = "Скорость транзакций в секунду", example = "50")
    int tps,
    @NotNull
    @Schema(
            description = """
                    Тип транзакции:
                    * `NORMAL` - Обычная дневная транзакция
                    * `HIGH_VALUE` - Транзакция на крупную сумму
                    * `ALMOST_DAILY_LIMIT` - Почти исчерпавшая лимит
                    * `BLOCKED` - Транзакция по заблокированной карте
                    * `NO_MONEY` - Транзакция больше чем баланс карты
                    * `MORE_THAN_DAILY_LIMIT` - Транзакция с превышением дневного лимита
                    * `INVALID_PAN` - Транзакция с несуществующими PAN""",
            example = "NORMAL"
    )
    TransactionType transactionType
) {}
