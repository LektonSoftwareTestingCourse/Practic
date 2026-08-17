package com.processing.terminalsimulator.dto;

import java.util.Map;

public record HealthResponse(
        String status,
        String service,
        Map<String, String> dependencies
) {}
