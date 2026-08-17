package com.processing.common.dto.annotations;

import com.processing.common.dto.annotations.validators.ExactSizeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates String value length
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExactSizeValidator.class)
@Documented
public @interface ExactSize {

    int value();

    String message() default "The length must be exactly {value} symbols";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
