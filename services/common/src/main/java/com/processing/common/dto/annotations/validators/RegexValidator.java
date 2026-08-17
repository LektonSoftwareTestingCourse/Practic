package com.processing.common.dto.annotations.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;

/**
 * Validates Regex annotations
 * @param <A> annotation type
 */
public abstract class RegexValidator<A extends Annotation> implements ConstraintValidator<A, String> {
    private final String regexp;
    private final boolean nullAllowed;

    /**
     * Initializes RegexValidator
     * @param regexp regular expression
     * @param nullAllowed can be null
     */
    protected RegexValidator(
        String regexp,
        boolean nullAllowed
    ) {
        this.regexp = regexp;
        this.nullAllowed = nullAllowed;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return nullAllowed;
        }
        return value.matches(regexp);
    }
}
