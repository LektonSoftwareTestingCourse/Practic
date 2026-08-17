package com.processing.merchantacquirer.controller.dto;

import com.processing.merchantacquirer.domain.model.ScenarioType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Запрос на запуск симуляции транзакций")
public record SimulatorRequest(
    @Schema(description = "Количество генерируемых транзакций", example = "50", minimum = "1")
    @Min(1) int count,

    @Schema(description = "Сценарий симуляции: определяет MCC по умолчанию, временной диапазон "
        + "и распределение сумм", example = "grocery", requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"grocery", "electronics", "restaurant", "travel"})
    @NotNull(message = "Scenario is required") ScenarioType scenario,

    @Schema(description = "Опциональное переопределение MCC-кодов. Если задано — имеет приоритет "
        + "над MCC сценария", example = "[\"5411\", \"5499\"]")
    List<String> mccCodes,

    @Schema(description = "Целевой темп отправки транзакций в Gateway (запросов в секунду). "
        + "Concurrency вычисляется как tps / 2. Если не задано — используется 100",
        example = "100", minimum = "1")
    @Min(value = 1, message = "tps must be >= 1") Integer tps) {

  public SimulatorRequest {
    if (tps == null) {
      tps = 100;
    }
  }
}
