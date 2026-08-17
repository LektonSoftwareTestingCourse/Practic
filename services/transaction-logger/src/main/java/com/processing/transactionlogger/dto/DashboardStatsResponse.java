package com.processing.transactionlogger.dto;

import java.math.BigDecimal;

/**
 * Агрегированная статистика по всем транзакциям для Dashboard.
 *
 * @param totalTransactions    общее число транзакций
 * @param approvedCount        число одобренных
 * @param declinedCount        число отклонённых
 * @param approvalRate         доля одобренных (0.0–1.0, например 0.88 = 88%)
 * @param totalAmount          суммарный объём всех транзакций в копейках/центах
 * @param averageAmount        средняя сумма транзакции в копейках/центах
 * @param avgProcessingTimeMs  среднее время обработки в миллисекундах
 * @param transactionsPerMinute количество транзакций за последнюю минуту
 */
public record DashboardStatsResponse(
        long totalTransactions,
        long approvedCount,
        long declinedCount,
        double approvalRate,
        BigDecimal totalAmount,
        BigDecimal averageAmount,
        double avgProcessingTimeMs,
        long transactionsPerMinute
) {
}
