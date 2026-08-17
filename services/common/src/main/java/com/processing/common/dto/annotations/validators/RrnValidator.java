package com.processing.common.dto.annotations.validators;

import com.processing.common.dto.annotations.Rrn;

/**
 * Validates RRN number
 */
public class RrnValidator extends RegexValidator<Rrn> {

    public RrnValidator() {
        super("^\\d{12}$", true);
    }
}
