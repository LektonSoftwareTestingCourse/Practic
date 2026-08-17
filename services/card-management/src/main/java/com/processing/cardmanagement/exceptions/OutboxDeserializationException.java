package com.processing.cardmanagement.exceptions;

public final class OutboxDeserializationException extends CardManagementException {

    public OutboxDeserializationException(String eventType) {
        super("Failed to deserialize event: " + eventType);
    }
}
