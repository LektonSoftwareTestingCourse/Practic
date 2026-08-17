package com.processing.common.dto.cardmanagement;

import com.processing.common.dto.annotations.NotNegative;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;

@Schema(description = "Request to update a card")
public record PatchCardRequest(

    @Nullable
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

    @Nullable
    @NotNegative
    @Schema(description = "Daily limit in kopecks", example = "15000000")
    BigDecimal dailyLimit,

    @Nullable
    @NotNegative
    @Schema(description = "Monthly limit in kopecks", example = "300000000")
    BigDecimal monthlyLimit,

    @Nullable
    @Schema(description = "Available balance in kopecks", example = "1000000000")
    BigDecimal availableBalance
) {}
