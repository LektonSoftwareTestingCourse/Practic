package com.processing.authorization.dto;

/**
 * Record для представления ответа health-эндпоинта внешнего сервиса.
 *
 * @param service имя сервиса (например, "card-management")
 * @param status  статус сервиса (например, "ok", "down", "degraded")
 */
public record Response(String service, String status) {
}
