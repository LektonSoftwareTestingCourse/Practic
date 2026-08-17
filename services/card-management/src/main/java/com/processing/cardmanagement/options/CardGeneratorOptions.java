package com.processing.cardmanagement.options;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Конфигурация генератора тестовых карт
 * Загружается из application.properties с префиксом app.card-generator
 */
@Validated
@ConfigurationProperties(prefix = "app.card-generator")
public record CardGeneratorOptions(
        BigDecimal minBalance,
        BigDecimal maxBalance,
        BigDecimal minDailyLimit,
        BigDecimal maxDailyLimit,
        String currencyCode,
        int maxCount
) {
}
