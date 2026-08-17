package com.processing.authorization.events;

public record AuthServiceAuthDeclinedLimitsEvent(String pan) implements AuthorizationEvent {
}
