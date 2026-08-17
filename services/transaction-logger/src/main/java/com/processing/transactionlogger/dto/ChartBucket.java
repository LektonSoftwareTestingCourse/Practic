package com.processing.transactionlogger.dto;

import com.processing.transactionlogger.repository.ChartBucketRow;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Одна временная корзина агрегации для графиков Dashboard.
 *
 * @param timestamp начало интервала (усечённое до часа или дня, UTC)
 * @param total     всего транзакций в интервале
 * @param approved  одобренных транзакций
 * @param declined  отклонённых транзакций
 * @param amount    суммарный объём транзакций в минорных единицах валюты
 */
public record ChartBucket(
        Instant timestamp,
        long total,
        long approved,
        long declined,
        BigDecimal amount
) {
    public static ChartBucket from(ChartBucketRow row) {
        return new ChartBucket(
                row.getBucket(),
                row.getTotal(),
                row.getApproved(),
                row.getDeclined(),
                row.getAmount()
        );
    }
}
