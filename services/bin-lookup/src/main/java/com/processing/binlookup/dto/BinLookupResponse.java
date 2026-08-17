package com.processing.binlookup.dto;

/**
 * Response DTO returned by the BIN lookup endpoint.
 *
 * @param bin       6-digit BIN (Bank Identification Number)
 * @param issuerId  unique issuer identifier
 * @param issuerName human-readable issuer name
 */
public record BinLookupResponse(
        String bin,
        String issuerId,
        String issuerName) {
}
