package com.processing.transactionlogger.specification;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.Instant;

/**
 * Параметры запроса агрегированных данных для графиков Dashboard.
 * Все поля опциональны: при отсутствии {@code granularity} используется почасовая
 * группировка, при отсутствии границ диапазона агрегируются все транзакции.
 */
@Data
@ValidDateRange
public class ChartsFilter {
    /** Гранулярность агрегации по времени: {@code hour} (по умолчанию) или {@code day} */
    @Pattern(regexp = "hour|day", message = "granularity must be hour or day")
    private String granularity = "hour";

    /** Нижняя граница диапазона по {@code createdAt} (включительно) */
    private Instant from;

    /** Верхняя граница диапазона по {@code createdAt} (исключительно) */
    private Instant to;
}
