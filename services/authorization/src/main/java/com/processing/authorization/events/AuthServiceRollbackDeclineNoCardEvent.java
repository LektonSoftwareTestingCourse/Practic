package com.processing.authorization.events;

public record AuthServiceRollbackDeclineNoCardEvent(String pan) implements AuthorizationEvent {
}
