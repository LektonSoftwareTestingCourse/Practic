package com.processing.authorization.events;

public record AuthServiceAuthDeclinedCardExpiryDateEvent(String pan, String date) implements AuthorizationEvent {
}
