package com.processing.merchantacquirer.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Schema(description = "Параметры сценария симуляции (MCC по умолчанию, диапазон сумм, "
    + "временное окно и доля одобрений)")
public class Scenario {
  @Schema(description = "MCC-коды по умолчанию для сценария", example = "[\"5411\", \"5499\"]")
  private List<String> mcc;

  @Schema(description = "Нижняя граница суммы транзакции в копейках", example = "10000")
  private BigDecimal countLower;

  @Schema(description = "Верхняя граница суммы транзакции в копейках", example = "300000")
  private BigDecimal countUpper;

  @Schema(description = "Начало временного окна транзакций (HH:mm)", example = "08:00")
  private String timeLower;

  @Schema(description = "Конец временного окна транзакций (HH:mm)", example = "22:00")
  private String timeUpper;

  @Schema(description = "Целевая доля одобренных транзакций в процентах", example = "95")
  private int avgApproved;
}
