package com.processing.gateway.health.models;

import java.util.Map;

/**
 * Aggregated health-check response returned by the gateway.
 *
 * @param status overall gateway status
 * @param service service name
 * @param version gateway version
 * @param services downstream service statuses keyed by service name
 */
public record HealthResponse(
        HealthStatus status,
        String service,
        String version,
        Map<String, HealthStatus> services
) {}
