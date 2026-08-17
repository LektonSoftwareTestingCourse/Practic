package com.processing.authorization.events;

public record AuthServiceAuthDeclineNoCardEvent(String pan) implements AuthorizationEvent {
}
