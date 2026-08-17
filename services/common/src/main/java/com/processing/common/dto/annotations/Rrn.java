package com.processing.common.dto.annotations;

import com.processing.common.dto.annotations.validators.RrnValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates Rrn number
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RrnValidator.class)
@Documented
public @interface Rrn {

    String message() default "RRN number must contain exactly 12 digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
