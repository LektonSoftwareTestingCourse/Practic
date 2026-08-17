package com.processing.authorization.events;

public record AuthServiceAuthDeclinedCardStatusEvent(String pan, String cardStatus) implements AuthorizationEvent {
}
