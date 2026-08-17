package com.processing.cardmanagement.exceptions;

public final class MassiveCardCreationCollisionException extends CardManagementException {

    public MassiveCardCreationCollisionException() {
        super("Collision happened while saving batch of cards");
    }
}
