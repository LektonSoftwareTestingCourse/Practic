package com.processing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationRequestNormalizerTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldKeepTerminalIdWhenItIsNullOrBlank(String terminalId) {
        assertThat(AuthorizationRequestNormalizer.normalizeTerminalId(terminalId))
                .isEqualTo(terminalId);
    }

    @Test
    void shouldPadTerminalIdWhenItIsShorterThanEightCharacters() {
        assertThat(AuthorizationRequestNormalizer.normalizeTerminalId("123"))
                .isEqualTo("123" + "0".repeat(5));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldKeepMerchantIdWhenItIsNullOrBlank(String merchantId) {
        assertThat(AuthorizationRequestNormalizer.normalizeMerchantId(merchantId))
                .isEqualTo(merchantId);
    }

    @Test
    void shouldPadMerchantIdWhenItIsShorterThanFifteenCharacters() {
        assertThat(AuthorizationRequestNormalizer.normalizeMerchantId("M1"))
                .isEqualTo("M1" + "0".repeat(13));
    }

}
