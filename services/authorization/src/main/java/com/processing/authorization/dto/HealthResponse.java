package com.processing.authorization.dto;

import java.util.Map;

/**
 * Record для представления ответа health-эндпоинта внешнего сервиса в контроллере.
 *
 * @param service имя сервиса ("authorization")
 * @param status  статус сервиса (например, "ok", "down", "degraded")
 * @param dependencies ответы health-эндпоинта зависимых сервисов
 */
public record HealthResponse(
        String status,
        String service,
        Map<String, String> dependencies
) {}
