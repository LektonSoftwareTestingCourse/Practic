package com.processing.common.dto.authorization;

import com.processing.common.dto.annotations.ExactSize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

import java.math.BigDecimal;

@Builder
public record AuthorizationRequest(
        @NotBlank
        @ExactSize(4)
        @Schema(description = "Request code", example = "0100")
        String mti,

        @NotBlank
        @ExactSize(6)
        @Schema(description = "System trace audit number", example = "000000")
        String stan,

        @NotBlank
        @ExactSize(16)
        @Schema(description = "Number card", example = "4000003458730237")
        String pan,

        @NotBlank
        @ExactSize(6)
        @Schema(description = "Processing code", example = "000000")
        String processingCode,

        @NotNull
        @PositiveOrZero
        @Schema(description = "Amount", example = "532.1940364669593")
        BigDecimal amount,

        @NotBlank
        @ExactSize(3)
        @Schema(description = "Currency type", example = "643")
        String currencyCode,

        @NotNull
        @Schema(description = "Transaction time", example = "2026-06-05T18:12:49.07Z")
        Instant transmissionDateTime,

        @NotBlank
        @ExactSize(8)
        @Schema(description = "Terminal id", example = "TERM0001")
        String terminalId,

        @Schema(description = "Terminal type", example = "POS")
        String terminalType,

        @NotBlank
        @ExactSize(15)
        @Schema(description = "Merchant ID", example = "MERCH0000000002")
        String merchantId,

        @NotBlank
        @ExactSize(4)
        @Schema(description = "Merchant category code", example = "5045")
        String mcc,

        @NotBlank
        @Schema(description = "Acquirer ID", example = "ACQ002")
        String acquirerId,

        @NotBlank
        @Schema(description = "Issuer ID, set by Switch after BIN routing")
        String issuerId
) {
        public AuthorizationRequest withIssuerId(String issuerId) {
                return new AuthorizationRequest(
                        mti, stan, pan, processingCode, amount, currencyCode,
                        transmissionDateTime, terminalId, terminalType, merchantId, mcc, acquirerId, issuerId);
        }


        public AuthorizationRequest forReversal(String rrn) {
                return new AuthorizationRequest(
                        "0400", stan, pan, processingCode, amount, currencyCode,
                        transmissionDateTime, terminalId, terminalType, merchantId, mcc, acquirerId, issuerId);
        }
}
