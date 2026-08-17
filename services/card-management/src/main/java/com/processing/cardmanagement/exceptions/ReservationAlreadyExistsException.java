package com.processing.cardmanagement.exceptions;

import static com.processing.common.utils.MaskPan.maskPan;

public final class ReservationAlreadyExistsException extends CardManagementException {

    public ReservationAlreadyExistsException(String rrn, String pan) {
        super("Reservation with RRN number \""
            + rrn
            + "\" and PAN \""
            + maskPan(pan)
            + "\" already exists in the database");
    }
}
