package com.processing.merchantacquirer.controller;

import com.processing.common.dto.ErrorResponse;
import com.processing.merchantacquirer.controller.dto.HealthResponse;
import com.processing.merchantacquirer.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Health", description = "Проверка работоспособности сервиса")
public class HealthController {
  private final SimulationService simulationService;

  @Operation(
      summary = "Health-check сервиса",
      description = "Возвращает статус сервиса и количество загруженных мерчантов. "
          + "Обращается к БД для подсчёта мерчантов.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Сервис работает",
          content = @Content(schema = @Schema(implementation = HealthResponse.class))),
      @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервиса "
          + "(например, недоступна БД при подсчёте мерчантов)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    return ResponseEntity.ok(new HealthResponse("ok", "merchant-acquirer-simulator", simulationService.countMerchants()));
  }
}
