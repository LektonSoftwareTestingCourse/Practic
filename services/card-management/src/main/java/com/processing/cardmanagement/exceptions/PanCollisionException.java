package com.processing.cardmanagement.exceptions;

import static com.processing.common.utils.MaskPan.maskPan;

public final class PanCollisionException extends CardManagementException {

    public PanCollisionException(String pan) {
        super("Card with PAN \"" + maskPan(pan) + "\" already exists");
    }
}
