package com.processing.dto;

import java.util.Map;

/**
 * Ответ эндпоинта {@code GET /health} сервиса Switch.
 *
 * @param status       общий статус сервиса ({@code "ok"})
 * @param service      имя сервиса ({@code "switch"})
 * @param version      версия из конфигурации
 * @param dependencies статусы downstream-зависимостей
 */
public record HealthResponse(
        String status,
        String service,
        String version,
        Map<String, String> dependencies
) {}
