package com.processing.common.dto.authorization;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.Duration;

public record AuthorizationResponse(
        @Schema(description = "Response code", example = "0110")
        String mti,

        @Schema(description = "System trace audit number", example = "000301")
        String stan,

        @Schema(description = "Retrieval reference number", example = "012345678901")
        String rrn,

        @Schema(description = "Code authorization", example = "TEST01")
        String authCode,

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

    public static final String CODE_APPROVED = "00";

    public static final String CODE_DECLINED_GENERAL = "05";

    public static final String CODE_CARD_NOT_FOUND = "14";

    public static final String CODE_INSUFFICIENT_FUNDS = "51";

    public static final String CODE_CARD_EXPIRED = "54";

    public static final String CODE_EXCEEDS_LIMIT = "61";

    public static final String CODE_SERVICE_UNAVAILABLE = "96";

    public static AuthorizationResponse unknownBin(String stan) {
        return new AuthorizationResponse(
                "0110", stan, null, null, "14", "DECLINED",
                "CARD_NOT_FOUND", 0
        );
    }

    public static AuthorizationResponse authUnavailable(String stan) {
        return new AuthorizationResponse(
                "0110", stan, null, null, "05", "DECLINED",
                "Authorization service unavailable", 0
        );
    }

    public static AuthorizationResponse systemError(String stan) {
        return new AuthorizationResponse(
                "0110", stan, null, null, "96", "DECLINED",
                "System error", 0
        );
    }

    public static AuthorizationResponse approved(AuthorizationRequest request, String rrn,
                                                 String authCode, Instant requestInputTime) {
        return new AuthorizationResponse(
                request.mti(),
                request.stan(),
                rrn,
                authCode,
                CODE_APPROVED,
                STATUS_APPROVED,
                null,
                Math.toIntExact(Duration.between(requestInputTime, Instant.now()).toMillis())
        );
    }

    public static AuthorizationResponse declined(AuthorizationRequest request, String reason,
                                                 String code, Instant requestInputTime) {
        return new AuthorizationResponse(
                request.mti(),
                request.stan(),
                null,
                null,
                code,
                STATUS_DECLINED,
                reason,
                Math.toIntExact(Duration.between(requestInputTime, Instant.now()).toMillis())
        );
    }
}
