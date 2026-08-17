package com.processing.common.dto.cardmanagement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response for card generation")
public record GenerateCardResponse(
    @Schema(description = "Number of generated cards", example = "100")
    int generated,
    @Schema(description = "List of generated cards")
    List<CardModel> cards) {}
