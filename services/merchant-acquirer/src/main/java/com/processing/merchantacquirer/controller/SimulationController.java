package com.processing.merchantacquirer.controller;

import com.processing.common.dto.ErrorResponse;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.controller.dto.SimulatorRequest;
import com.processing.merchantacquirer.controller.dto.SimulatorResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.service.SimulationService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@OpenAPIDefinition(
        info = @Info(
                title = "Merchant + Acquirer Simulator API",
                version = "1.0.0",
                description = """
            Эмулятор поведения торговых точек (мерчантов) и банков-эквайреров.
            Генерирует транзакции по MCC-кодам и сценариям, рассчитывает комиссию
            эквайрера и отправляет авторизационные запросы в Gateway.
            """,
                contact = @Contact(name = "Merchant Acquirer Simulator")
        )
)
@Tag(name = "Simulator", description = "Запуск симуляции транзакций, список мерчантов и расчёт комиссии эквайрера")
public class SimulationController {
  private final SimulationService simulationService;

  @Operation(
      summary = "Запустить симуляцию транзакций",
      description = """
          Генерирует `count` транзакций по выбранному сценарию и отправляет их в Gateway.

          Правило приоритета MCC:
          - если передан `mccCodes` — используются явно указанные MCC-коды, `scenario`
            при этом определяет только временной диапазон и распределение сумм;
          - если `mccCodes` не указан — берутся MCC по умолчанию для заданного `scenario`.

          Для каждой транзакции рассчитывается справочная комиссия эквайрера и сохраняется в БД.
          """)
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Симуляция выполнена, возвращена статистика approved/declined",
          content = @Content(schema = @Schema(implementation = SimulatorResponse.class))),
      @ApiResponse(responseCode = "400", description = """
          Некорректный запрос:
          - `count` меньше 1 либо `scenario` не указан (ошибка валидации);
          - тело запроса не читается / неизвестное значение `scenario` (нечитаемый JSON);
          - недопустимый аргумент при обработке запроса.
          """,
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Не найдены карты для генерации "
          + "либо мерчанты с заданными MCC-кодами",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "502", description = "Ошибка внешнего сервиса "
          + "(Card management или API Gateway недоступны/вернули ошибку)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервиса "
          + "(в т.ч. прерывание потока при параллельной отправке транзакций)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/api/simulator/merchant/run")
  public ResponseEntity<SimulatorResponse> run(@RequestBody @Valid SimulatorRequest request) {
    SimulatorResponse response = simulationService.run(request);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Получить список всех мерчантов",
      description = "Возвращает полный список мерчантов, хранящихся в базе данных.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список мерчантов получен",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = Merchant.class)))),
      @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервиса "
          + "(например, недоступна БД)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/api/simulator/merchants")
  public ResponseEntity<List<Merchant>> merchants() {
    return ResponseEntity.ok(simulationService.getAllMerchants());
  }

  @Operation(
      summary = "Получить рассчитанную комиссию эквайрера",
      description = "Возвращает справочную комиссию эквайрера для ранее сгенерированной транзакции, "
          + "найденной по совокупности полей: дата/время передачи, STAN, terminalId, сумма и PAN.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Комиссия найдена",
          content = @Content(schema = @Schema(implementation = AcquirerFeeResponse.class))),
      @ApiResponse(responseCode = "400", description = """
          Некорректный запрос:
          - не заполнены обязательные поля (`transmissionDateTime`, `pan`, `stan`, `terminalId`);
          - тело запроса не читается (нечитаемый JSON).
          """,
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Комиссия эквайрера для заданных параметров не найдена",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервиса",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/api/simulator/merchant/fee")
  public ResponseEntity<AcquirerFeeResponse> getAcquirerFee(@RequestBody @Valid AcquirerFeeRequest request) {
    return ResponseEntity.ok(simulationService.getAcquirerFee(request));
  }
}
