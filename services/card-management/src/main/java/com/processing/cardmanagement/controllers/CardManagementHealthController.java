package com.processing.cardmanagement.controllers;

import com.processing.cardmanagement.models.CardManagementHealthResponse;
import com.processing.cardmanagement.services.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoint")
public class CardManagementHealthController {

    private final CardService cardService;

    @Operation(description = "Check service health")
    @ApiResponse(responseCode = "200", description = "Service is helthy")
    @GetMapping("/health")
    public ResponseEntity<CardManagementHealthResponse> health() {
        return ResponseEntity.ok(new CardManagementHealthResponse(
            "ok",
            "card-management",
            cardService.countAllCardsAndUpdate()
        ));
    }
}
