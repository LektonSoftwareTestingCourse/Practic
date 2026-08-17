package com.processing.authorization.events;

public record AuthServiceAuthDeclineNoServiceEvent(
        String pan,
        String unavailableService,
        String reason) implements AuthorizationEvent {
}
