package com.processing.cardmanagement.controllers;

import com.processing.cardmanagement.mappers.CardRestMapper;
import com.processing.cardmanagement.mappers.CardStatusMapper;
import com.processing.cardmanagement.services.CardService;
import com.processing.common.dto.ErrorResponse;
import com.processing.common.dto.annotations.Bin;
import com.processing.common.dto.annotations.NotNegative;
import com.processing.common.dto.annotations.Pan;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.cardmanagement.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/cards")
@Validated
@Tag(name = "Cards", description = "Card management endpoints")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardRestMapper restMapper;
    private final CardStatusMapper cardStatusMapper;

    @Operation(summary = "Create a new card")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Card create successfully",
                    content = @Content(schema = @Schema(implementation = CardModel.class))),
            @ApiResponse(responseCode = "400", description = "invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CardModel> createCard(@Valid @RequestBody CreateCardRequest data) {
        var card = cardService.createCard(
                data.bin(),
                data.cardholderName(),
                data.currencyCode(),
                data.dailyLimit(),
                data.monthlyLimit(),
                data.initialBalance()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(restMapper.toDto(card));
    }

    @Operation(summary = "Get card by PAN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card found",
                    content = @Content(schema = @Schema(implementation = CardModel.class))),
            @ApiResponse(responseCode = "400", description = "Invalid PAN format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{pan}")
    public ResponseEntity<CardModel> getCard(@PathVariable @Pan String pan) {
        return ResponseEntity.ok(restMapper.toDto(cardService.getCard(pan)));
    }

    @Operation(summary = "Get list of cards with pagination and filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetCardsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public ResponseEntity<GetCardsResponse> getCards(

            @Nullable
            @Positive
            @Schema(description = "Number of cards per page", example = "10")
            @RequestParam(required = false, defaultValue = "10")
            Integer limit,

            @Nullable
            @NotNegative
            @Schema(description = "Offset for pagination", example = "0")
            @RequestParam(required = false, defaultValue = "0")
            Long offset,

            @Nullable
            @Schema(
                    description = "Card status",
                    example = "ACTIVE",
                    allowableValues = {
                            "ACTIVE",
                            "INACTIVE",
                            "BLOCKED",
                            "EXPIRED",
                            "DELETED"
                    }
            )
            @RequestParam(required = false)
            CardModelStatus status,

            @Nullable
            @Bin
            @Schema(description = "Bank Identification Number (BIN)", example = "400000")
            @RequestParam(required = false)
            String bin,

            @Nullable
            @Size(min = 1, max = 10)
            @Pattern(regexp = "^[A-Z0-9]+$")
            @Schema(description = "Issuer ID", example = "ZZZZZZ")
            @RequestParam(required = false)
            String issuerId,

            @Nullable
            @Schema(description = "Start date")
            @RequestParam(required = false)
            Instant startDate,

            @Nullable
            @Schema(description = "End date")
            @RequestParam(required = false)
            Instant endDate
    ) {
        var domainStatus = cardStatusMapper.toCardStatus(status);
        var cards = cardService.getCards(
                        limit,
                        offset,
                        domainStatus,
                        bin,
                        issuerId,
                        startDate,
                        endDate
                )
                .stream()
                .map(restMapper::toDto)
                .toList();

        var total = cardService.countCardsFiltered(
                domainStatus,
                bin,
                issuerId,
                startDate,
                endDate
        );

        return ResponseEntity.ok(
                new GetCardsResponse(
                        total,
                        cards
                )
        );
    }

    @Operation(summary = "Partially update a card")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card update successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{pan}")
    public ResponseEntity<CardModel> patchCard(
            @PathVariable @Pan String pan,
            @Valid @RequestBody PatchCardRequest data
    ) {
        return ResponseEntity.ok(
                restMapper.toDto(cardService.patchCard(
                        pan,
                        cardStatusMapper.toCardStatus(data.status()),
                        data.dailyLimit(),
                        data.monthlyLimit(),
                        data.availableBalance()
                ))
        );
    }

    @Operation(summary = "Delete a card (sets status to DELETED)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid PAN format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{pan}")
    public ResponseEntity<Void> deleteCard(@PathVariable @Pan String pan) {
        cardService.deleteCard(pan);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reserve funds on a card")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funds reserve successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "402", description = "Insufficient funds",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "RRN is already in database or illegal state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{pan}/reserve")
    public ResponseEntity<CardModel> reserve(
            @PathVariable @Pan String pan,
            @Valid @RequestBody ReserveRequest data
    ) {
        return ResponseEntity.ok(
                restMapper.toDto(cardService.reserve(
                        pan,
                        data.amount(),
                        data.rrn()
                ))
        );
    }

    @Operation(summary = "Rollback funds on a card")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funds reserve successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Rollback already satisfied or illegal state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{pan}/rollback")
    public ResponseEntity<CardModel> rollback(
            @PathVariable @Pan String pan,
            @Valid @RequestBody RollbackRequest data
    ) {
        return ResponseEntity.ok(
                restMapper.toDto(cardService.rollback(
                        pan,
                        data.amount(),
                        data.rrn()
                ))
        );
    }

    @PostMapping("/bulk-update")
    public ResponseEntity<BulkUpdatedResponse> bulkUpdate(
            @Valid @RequestBody BulkUpdateRequest request
    ) {
        int updated = cardService.bulkUpdateStatus(
                request.bins(),
                request.pans(),
                cardStatusMapper.toCardStatus(request.status())
        );
        return ResponseEntity.ok(new BulkUpdatedResponse(updated));
    }
}
