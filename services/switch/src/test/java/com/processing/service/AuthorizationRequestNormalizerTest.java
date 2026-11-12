package com.processing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthorizationRequestNormalizerTest {
    @ParameterizedTest(name = "terminal {0} -> {1}")
    @CsvSource({
            "TERM0001, TERM0001",
            "TERM, TERM0000",
            "ABCDEFGHIJ, ABCDEFGH",
            "12345678, 12345678"
    })
    void shouldNormalizeTerminalId(String input, String expected) {
        assertEquals(expected, AuthorizationRequestNormalizer.normalizeTerminalId(input));
    }

    @Test
    void shouldPreserveNullAndBlankTerminalIds() {
        assertNull(AuthorizationRequestNormalizer.normalizeTerminalId(null));
        assertEquals("   ", AuthorizationRequestNormalizer.normalizeTerminalId("   "));
    }

    @ParameterizedTest(name = "merchant {0} -> {1}")
    @CsvSource({
            "MERCH0000000002, MERCH0000000002",
            "MERCH00000000002, MERCH0000000002",
            "OTHER00000000001, OTHER0000000000",
            "MERCH12345678901, MERCH1234567890",
            "MERCH123456789012345, MERCH1234567890",
            "SHOP, SHOP00000000000"
    })
    void shouldNormalizeMerchantId(String input, String expected) {
        assertEquals(expected, AuthorizationRequestNormalizer.normalizeMerchantId(input));
    }

    @Test
    void shouldPreserveNullAndBlankMerchantIds() {
        assertNull(AuthorizationRequestNormalizer.normalizeMerchantId(null));
        assertEquals("   ", AuthorizationRequestNormalizer.normalizeMerchantId("   "));
    }
}
