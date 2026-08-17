package com.processing.common.dto.annotations;

import com.processing.common.dto.annotations.validators.PanValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates pan number
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PanValidator.class)
@Documented
public @interface Pan {

    String message() default "PAN number must contain exactly 16 digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
