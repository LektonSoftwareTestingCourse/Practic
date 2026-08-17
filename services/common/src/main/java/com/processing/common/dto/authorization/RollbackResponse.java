package com.processing.common.dto.authorization;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;

public record RollbackResponse(
        @Schema(description = "Retrieval reference number", example = "012345678901")
        String rrn,

        @Schema(description = "Response code", example = "00")
        String responseCode,

        @Schema(description = "Status", example = "APPROVED")
        String status,

        @Schema(description = "Reason for refusal")
        String declineReason,

        @Schema(description = "Processing time", example = "67")
        Integer processingTimeMs
) {
    public static final String STATUS_APPROVED = "APPROVED";

    public static final String STATUS_DECLINED = "DECLINED";

    public static final String CODE_SUCCESS = "00";

    public static final String CODE_DECLINED_GENERAL = "05";

    public static final String CODE_TRANSACTION_NOT_FOUND = "14";

    public static final String CODE_SERVICE_UNAVAILABLE = "96";

    public static RollbackResponse approved(RollbackRequest request, Instant requestInputTime) {
        return new RollbackResponse(
                request.rrn(),
                CODE_SUCCESS,
                STATUS_APPROVED,
                null,
                Math.toIntExact(Duration.between(requestInputTime, Instant.now()).toMillis())
        );
    }

    public static RollbackResponse declined(RollbackRequest request, String reason, String code, Instant requestInputTime) {
        return new RollbackResponse(
                request.rrn(),
                code,
                STATUS_DECLINED,
                reason,
                Math.toIntExact(Duration.between(requestInputTime, Instant.now()).toMillis())
        );
    }
}
