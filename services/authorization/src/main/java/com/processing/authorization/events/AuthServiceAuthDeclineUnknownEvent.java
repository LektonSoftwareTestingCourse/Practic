package com.processing.authorization.events;

public record AuthServiceAuthDeclineUnknownEvent(String pan, String reason) implements AuthorizationEvent {
}
