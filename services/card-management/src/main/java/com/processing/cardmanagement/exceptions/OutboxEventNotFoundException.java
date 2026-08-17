package com.processing.cardmanagement.exceptions;

import java.util.UUID;

public final class OutboxEventNotFoundException extends CardManagementException {

    public OutboxEventNotFoundException(UUID id) {
        super("Outbox event not found: " + id);
    }
}
