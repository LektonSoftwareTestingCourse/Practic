package com.processing.cardmanagement.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Health check response")
public record CardManagementHealthResponse(
    @Schema(description = "Service status", example = "ok")
    String status,
    @Schema(description = "Service name", example = "card-management")
    String service,
    @Schema(description = "Number of cards in database", example = "1000")
    long cardsInDatabase
) {}
