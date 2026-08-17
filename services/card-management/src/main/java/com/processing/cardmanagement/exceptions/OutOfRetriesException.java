package com.processing.cardmanagement.exceptions;

import lombok.Getter;

public final class OutOfRetriesException extends RuntimeException {

    @Getter
    private final Exception reason;

    public OutOfRetriesException(int retries, Exception ex) {
        super("Operation failed (retries > " + retries + ". Reason: " + ex.getClass().getName() + ": " + ex.getMessage());
        this.reason = ex;
    }
}
