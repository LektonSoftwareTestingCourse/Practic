package com.processing.authorization.events;

public record AuthServiceRollbackDeclineConflictEvent(String pan) implements AuthorizationEvent {
}
