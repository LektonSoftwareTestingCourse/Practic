package com.processing.authorization.events;

public record AuthServiceRollbackDeclineNoServiceEvent(
        String pan,
        String unavailableService,
        String reason) implements AuthorizationEvent {
}
