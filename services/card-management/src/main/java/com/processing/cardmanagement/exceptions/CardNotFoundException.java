package com.processing.cardmanagement.exceptions;

import static com.processing.common.utils.MaskPan.maskPan;

public final class CardNotFoundException extends CardManagementException {

    public CardNotFoundException(String pan) {
        super("Card with PAN " + maskPan(pan) + " was not found");
    }
}
