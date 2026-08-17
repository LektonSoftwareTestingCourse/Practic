package com.processing.authorization.events;

public record CmsClientRollbackEvent(String pan) implements AuthorizationEvent {
}
