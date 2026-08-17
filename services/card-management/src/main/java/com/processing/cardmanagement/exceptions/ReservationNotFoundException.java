package com.processing.cardmanagement.exceptions;

import static com.processing.common.utils.MaskPan.maskPan;

public final class ReservationNotFoundException extends CardManagementException {

    public ReservationNotFoundException(String rrn, String pan) {
        super("Can not find reservation with RRN \""
            + rrn
            + "\" and PAN \""
            + maskPan(pan)
            + "\""
        );
    }
}
