package com.processing.common.dto;

import java.time.Instant;

public record ErrorResponse(
    String error,
    String message,
    Instant timestamp,
    String serviceName,
    String retryAfterMs
) {}
