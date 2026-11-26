package com.processing.cardmanagement.services;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuhnValidatorTest {
    private final LuhnValidator validator = new LuhnValidator();

    @ParameterizedTest(name = "PAN {0} valid={1}")
    @CsvSource({
            "4111111111111111, true",
            "4242424242424242, true",
            "5555555555554444, true",
            "4111111111111112, false",
            "4242424242424241, false"
    })
    void shouldValidatePanUsingLuhnAlgorithm(String pan, boolean expected) {
        assertEquals(expected, validator.isValid(pan));
    }

    @Test
    void shouldGenerateSixteenDigitPanWithValidCheckDigit() {
        String pan = validator.generatePan("400000");

        assertEquals(16, pan.length());
        assertTrue(pan.startsWith("400000"));
        assertTrue(validator.isValid(pan));
    }
}
