package com.processing.common.dto.annotations.validators;

import com.processing.common.dto.annotations.Bin;

/**
 * Validates BIN number
 */
public class BinValidator extends RegexValidator<Bin> {

    public BinValidator() {
        super("^\\d{6}$", true);
    }
}
