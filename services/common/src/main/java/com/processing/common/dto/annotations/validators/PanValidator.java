package com.processing.common.dto.annotations.validators;

import com.processing.common.dto.annotations.Pan;

/**
 * Validates pan number
 */
public class PanValidator extends RegexValidator<Pan> {

    public PanValidator() {
        super("^\\d{16}$", true);
    }
}
