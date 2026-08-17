package com.processing.common.dto.cardmanagement;

import com.processing.common.dto.annotations.Rrn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request to reserve funds on a card")
public record ReserveRequest(

    @Positive
    @Schema(description = "Amount to reserve", example = "150000")
    BigDecimal amount,

    @NotBlank
    @Rrn
    @Schema(description = "Retrieval Reference Number", example = "012345678901")
    String rrn
) {}
