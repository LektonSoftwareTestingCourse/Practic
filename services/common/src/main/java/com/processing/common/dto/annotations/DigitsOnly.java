package com.processing.common.dto.annotations;

import com.processing.common.dto.annotations.validators.DigitsOnlyValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Checks if String value can contain only digits
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DigitsOnlyValidator.class)
@Documented
public @interface DigitsOnly {

    String message() default "Value can contain digits only";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
