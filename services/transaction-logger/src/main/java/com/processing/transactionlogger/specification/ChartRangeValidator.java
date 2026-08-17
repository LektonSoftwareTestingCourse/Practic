package com.processing.transactionlogger.specification;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Реализация {@link ValidDateRange} для {@link ChartsFilter}.
 * Проверяет, что {@code from} не позже {@code to}; если одна из границ не задана — валидно.
 */
public class ChartRangeValidator implements ConstraintValidator<ValidDateRange, ChartsFilter> {
    @Override
    public boolean isValid(ChartsFilter filter, ConstraintValidatorContext context) {
        if (filter.getFrom() == null || filter.getTo() == null) {
            return true;
        }
        return !filter.getFrom().isAfter(filter.getTo());
    }
}
