package com.processing.cardmanagement.exceptions;

public final class RollbackAlreadySatisfiedException extends CardManagementException {

    public RollbackAlreadySatisfiedException(String rrn) {
        super("Rollback with RRN \"" + rrn + "\" is already satisfied");
    }
}
