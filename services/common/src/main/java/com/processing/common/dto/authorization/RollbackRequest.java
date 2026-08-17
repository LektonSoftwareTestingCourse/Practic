package com.processing.common.dto.authorization;

import com.processing.common.dto.annotations.Pan;
import com.processing.common.dto.annotations.Rrn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RollbackRequest(
    @NotBlank
    @Rrn
    @Schema(description = "Retrieval reference number", example = "012345678901")
    String rrn,

    @NotBlank
    @Pan
    @Schema(description = "Card number", example = "4000001234560001")
    String pan,

    @NotNull
    @Positive
    @Schema(description = "Rollback amount", example = "1000")
    BigDecimal amount
) {}
