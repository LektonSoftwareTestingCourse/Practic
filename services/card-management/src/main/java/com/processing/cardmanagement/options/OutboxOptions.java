package com.processing.cardmanagement.options;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Конфигурация Outbox
 * Загружается из application.properties с префиксом app.outbox
 */
@Validated
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxOptions(int intervalMs, int maxRetryCount) {
}
