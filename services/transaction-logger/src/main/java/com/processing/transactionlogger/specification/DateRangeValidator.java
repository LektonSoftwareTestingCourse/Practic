package com.processing.transactionlogger.specification;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Реализация валидатора для аннотации {@link ValidDateRange}
 * Проверяет, что {@code dateFrom} не позже {@code dateTo}
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, TransactionFilter> {
    @Override
    public boolean isValid(TransactionFilter filter, ConstraintValidatorContext context) {
        if (filter.getDateFrom() == null || filter.getDateTo() == null) {
            return true;
        }
        return !filter.getDateFrom().isAfter(filter.getDateTo());
    }
}
