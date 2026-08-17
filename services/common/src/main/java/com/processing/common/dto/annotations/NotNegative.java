package com.processing.common.dto.annotations;

import com.processing.common.dto.annotations.validators.NotNegativeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Checks that number value is not negative
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotNegativeValidator.class)
@Documented
public @interface NotNegative {

    String message() default "Value can not be negative";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
