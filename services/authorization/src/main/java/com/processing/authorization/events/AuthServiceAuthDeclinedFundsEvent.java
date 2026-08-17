package com.processing.authorization.events;

public record AuthServiceAuthDeclinedFundsEvent(String pan) implements AuthorizationEvent {
}
