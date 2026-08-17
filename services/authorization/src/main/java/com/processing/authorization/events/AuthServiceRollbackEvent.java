package com.processing.authorization.events;

public record AuthServiceRollbackEvent(String pan) implements AuthorizationEvent {
}
