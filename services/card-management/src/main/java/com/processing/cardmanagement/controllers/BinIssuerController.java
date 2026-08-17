package com.processing.cardmanagement.controllers;

import com.processing.cardmanagement.models.BinIssuer;
import com.processing.cardmanagement.services.BinIssuerService;
import com.processing.common.dto.ErrorResponse;
import com.processing.common.dto.cardmanagement.CreateBinIssuerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bins")
@RequiredArgsConstructor
@Tag(name = "Bins", description = "BIN to Issuer ID mapping")
public class BinIssuerController {
    private final BinIssuerService binIssuerService;

    @Operation(summary = "Get all BIN to Issuer ID mapping")
    @ApiResponse(responseCode = "200", description = "List of all mappings",
            content = @Content(schema = @Schema(implementation = BinIssuer.class)))
    @GetMapping
    public ResponseEntity<List<BinIssuer>> getAll() {
        return ResponseEntity.ok(binIssuerService.getAll());
    }

    @Operation(summary = "Get Issuer ID by BIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mapping found",
                    content = @Content(schema = @Schema(implementation = BinIssuer.class))),
            @ApiResponse(responseCode = "404", description = "BIN not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{bin}")
    public ResponseEntity<BinIssuer> getByBin(@PathVariable String bin) {
        String issuerId = binIssuerService.getIssuerId(bin);
        return ResponseEntity.ok(new BinIssuer(bin, issuerId));
    }

    @Operation(summary = "Create BIN to Issuer ID mapping")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mapping created",
                    content = @Content(schema = @Schema(implementation = BinIssuer.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "BIN already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<BinIssuer> create(@Valid @RequestBody CreateBinIssuerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(binIssuerService.create(request.bin(), request.issuerId()));
    }

    @Operation(summary = "Delete BIN to Issuer ID mapping")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mapping deleted"),
            @ApiResponse(responseCode = "404", description = "BIN not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{bin}")
    public ResponseEntity<Void> delete(@PathVariable String bin) {
        binIssuerService.delete(bin);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
