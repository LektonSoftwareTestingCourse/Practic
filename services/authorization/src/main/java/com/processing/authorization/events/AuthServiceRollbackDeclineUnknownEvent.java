package com.processing.authorization.events;

public record AuthServiceRollbackDeclineUnknownEvent(String pan, String reason) implements AuthorizationEvent {
}
