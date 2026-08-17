package com.processing.transactionlogger.repository;

import java.math.BigDecimal;

/**
 * Результат агрегирующего запроса {@link TransactionRepository#findStats}.
 * Должен быть интерфейсом: Spring Data создаёт JDK-прокси,
 * который маппит SQL-алиасы (например {@code total_amount}) на геттеры ({@code getTotalAmount}).
 */
public interface TransactionStats {
    long getTotal();
    long getApproved();
    long getDeclined();
    BigDecimal getTotalAmount();
    long getRecentCount();
    double getAvgProcessingTimeMs();
}
