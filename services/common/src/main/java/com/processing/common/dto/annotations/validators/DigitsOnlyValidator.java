package com.processing.common.dto.annotations.validators;

import com.processing.common.dto.annotations.DigitsOnly;

/**
 * Checks if String value can contain only digits
 */
public class DigitsOnlyValidator extends RegexValidator<DigitsOnly> {

    protected DigitsOnlyValidator() {
        super("^\\d*$", true);
    }
}
