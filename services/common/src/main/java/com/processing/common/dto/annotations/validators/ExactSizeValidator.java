package com.processing.common.dto.annotations.validators;

import com.processing.common.dto.annotations.ExactSize;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates String value length
 */
public class ExactSizeValidator implements ConstraintValidator<ExactSize, String> {

    private int targetSize;

    @Override
    public void initialize(ExactSize constraintAnnotation) {
        this.targetSize = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return value.length() == targetSize;
    }
}
