package com.processing.transactionlogger.repository;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Строка результата агрегации для графиков Dashboard
 * Имена геттеров соответствуют алиасам колонок нативного запроса
 */
public interface ChartBucketRow {
    Instant getBucket();
    Long getTotal();
    Long getApproved();
    Long getDeclined();
    BigDecimal getAmount();
}
