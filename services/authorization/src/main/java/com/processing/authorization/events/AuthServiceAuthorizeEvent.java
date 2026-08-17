package com.processing.authorization.events;

public record AuthServiceAuthorizeEvent(String pan) implements AuthorizationEvent {
}
