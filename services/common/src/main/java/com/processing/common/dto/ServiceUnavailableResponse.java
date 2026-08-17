package com.processing.common.dto;

public record ServiceUnavailableResponse(
        String error,
        String message,
        String serviceName
) {
}
