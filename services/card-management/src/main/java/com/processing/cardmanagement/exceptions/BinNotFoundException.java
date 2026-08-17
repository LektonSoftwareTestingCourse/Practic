package com.processing.cardmanagement.exceptions;

public final class BinNotFoundException extends CardManagementException {

    public BinNotFoundException(String bin) {
        super("BIN not found: " + bin);
    }
}
