package com.processing.cardmanagement.exceptions;

public final class InsufficientFundsException extends CardManagementException {

    public InsufficientFundsException() {
        super("Not enough funds on this account");
    }
}
