package com.processing.common.dto.annotations.validators;

import com.processing.common.dto.annotations.NotNegative;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * Checks that number value is not negative
 */
public class NotNegativeValidator implements ConstraintValidator<NotNegative, Number> {

    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BigDecimal bigDecimalValue = new BigDecimal(value.toString());
        return bigDecimalValue.signum() >= 0;
    }
}
