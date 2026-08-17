package com.processing.authorization.events;

public record CmsClientGetCardEvent(String pan) implements AuthorizationEvent {
}
