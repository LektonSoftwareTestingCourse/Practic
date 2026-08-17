package com.processing.authorization.events;

public record AuthServiceDuplicateKeyEvent(String pan, String reason) implements AuthorizationEvent {
}
