package com.processing.authorization.dto;

/**
 * Response DTO returned by the external BIN Lookup service.
 * Mirrors {@code com.processing.binlookup.dto.BinLookupResponse} for deserialization.
 *
 * @param bin       6-digit BIN
 * @param issuerId  unique issuer identifier
 * @param issuerName human-readable issuer name
 */
public record BinLookupResponse(
        String bin,
        String issuerId,
        String issuerName) {
}
