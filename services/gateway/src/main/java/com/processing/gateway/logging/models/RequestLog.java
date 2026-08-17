package com.processing.gateway.logging.models;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Data;

/**
 * Structured request log payload written by {@link com.processing.gateway.logging.RequestLoggingFilter}.
 */
@Data
@Builder
public class RequestLog {
    private String requestId;

    private String method;

    private String path;

    @JsonRawValue
    private String requestBody;

    private Integer responseCode;

    private Long responseTime;

    @JsonRawValue
    private String responseBody;
}
