package com.processing.common.dto.annotations;

import jakarta.validation.Payload;

public @interface IssuerId {
    String message() default "IssuerId must contain 6-10 uppercase letters and digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
