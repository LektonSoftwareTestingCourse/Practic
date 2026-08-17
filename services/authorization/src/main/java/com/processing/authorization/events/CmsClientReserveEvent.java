package com.processing.authorization.events;

public record CmsClientReserveEvent(String pan) implements AuthorizationEvent {
}
