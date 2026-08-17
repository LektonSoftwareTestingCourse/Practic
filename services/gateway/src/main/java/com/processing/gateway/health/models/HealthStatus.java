package com.processing.gateway.health.models;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Health state used for gateway and downstream service checks.
 */
public enum HealthStatus {
    OK,
    UNAVAILABLE,
    DEGRADED;

    @JsonValue
    @Override
    public String toString() {
        return super.name().toLowerCase();
    }
}
