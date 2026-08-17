package com.processing.common.dto.cardmanagement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "Card data transfer object")
public record CardModel(
        @Schema(description = "Unique identifier")
        UUID id,
        @Schema(description = "Card PAN", example = "4000001234567893")
        String pan,
        @Schema(description = "Bank Identification Number (BIN)", example = "400000")
        String bin,
        @Schema(description = "Cardholder name", example = "IVAN IVANOV")
        String cardholderName,
        @Schema(description = "Expiry date in MMyy format", example = "0629")
        YearMonth expiryDate,
        @Schema(
                description = "Card model status",
                example = "ACTIVE",
                allowableValues = {
                        "ACTIVE",
                        "INACTIVE",
                        "BLOCKED",
                        "EXPIRED"
                }
        )
        CardModelStatus status,
        @Schema(description = "Currency code", example = "643")
        String currencyCode,
        @Schema(description = "Daily limit in kopecks", example = "15000000")
        BigDecimal dailyLimit,
        @Schema(description = "Monthly limit in kopecks", example = "300000000")
        BigDecimal monthlyLimit,
        @Schema(description = "Available balance in kopecks", example = "1000000000")
        BigDecimal availableBalance,
        @Schema(description = "Issuer ID", example = "ZZZZZZ")
        String issuerId,
        @Schema(description = "Card creation date")
        Instant createdAt
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CardModel cardModel = (CardModel) o;

        return status == cardModel.status
                && Objects.equals(id, cardModel.id)
                && Objects.equals(pan, cardModel.pan)
                && Objects.equals(bin, cardModel.bin)
                && Objects.equals(issuerId, cardModel.issuerId)
                && Objects.equals(currencyCode, cardModel.currencyCode)
                && Objects.equals(expiryDate, cardModel.expiryDate)
                && Objects.equals(cardholderName, cardModel.cardholderName)
                && Objects.equals(createdAt, cardModel.createdAt)
                && (dailyLimit == null
                ? cardModel.dailyLimit == null
                : dailyLimit.compareTo(cardModel.dailyLimit) == 0)
                && (monthlyLimit == null
                ? cardModel.monthlyLimit == null
                : monthlyLimit.compareTo(cardModel.monthlyLimit) == 0)
                && (availableBalance == null
                ? cardModel.availableBalance == null
                : availableBalance.compareTo(cardModel.availableBalance) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                pan,
                bin,
                cardholderName,
                expiryDate,
                status,
                currencyCode,
                issuerId,
                createdAt,
                dailyLimit == null ? null : dailyLimit.stripTrailingZeros(),
                monthlyLimit == null ? null : monthlyLimit.stripTrailingZeros(),
                availableBalance == null ? null : availableBalance.stripTrailingZeros()
        );
    }

}
