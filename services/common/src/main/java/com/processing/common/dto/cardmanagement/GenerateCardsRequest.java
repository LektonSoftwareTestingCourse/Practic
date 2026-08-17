package com.processing.common.dto.cardmanagement;

import com.processing.common.dto.annotations.Bin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request to generate test cards")
public record GenerateCardsRequest(
    @Min(1)
    @Schema(description = "Number of cards to generate", example = "100")
    int count,

    @NotEmpty
    @Schema(description = "List of BINs to distribute cards")
    List<@Bin String> bins
) {}
