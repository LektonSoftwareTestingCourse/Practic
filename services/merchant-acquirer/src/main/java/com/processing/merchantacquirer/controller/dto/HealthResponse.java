package com.processing.merchantacquirer.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ health-check")
public record HealthResponse(
    @Schema(description = "Статус сервиса", example = "ok")
    String status,

    @Schema(description = "Имя сервиса", example = "merchant-acquirer-simulator")
    String service,

    @Schema(description = "Количество загруженных мерчантов", example = "20")
    long merchantsLoaded) {}
