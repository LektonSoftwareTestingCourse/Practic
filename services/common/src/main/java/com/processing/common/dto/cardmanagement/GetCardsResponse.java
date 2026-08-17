package com.processing.common.dto.cardmanagement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GetCardsResponse(
    @Schema(description = "Total number of cards matching filters", example = "100")
    long total,

    @Schema(description = "List of cards")
    List<CardModel> cards) {
}
