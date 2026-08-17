package com.processing.cardmanagement.exceptions;

public final class OutboxSerializationException extends CardManagementException {

    public OutboxSerializationException(String eventType) {
        super("Failed to serialize event: " + eventType);
    }
}
