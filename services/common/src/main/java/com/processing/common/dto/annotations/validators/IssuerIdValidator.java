package com.processing.common.dto.annotations.validators;

import com.processing.common.dto.annotations.IssuerId;

public class IssuerIdValidator extends RegexValidator<IssuerId> {
    public IssuerIdValidator() {
        super("[A-Z0-9]{6,10}", true);
    }
}
