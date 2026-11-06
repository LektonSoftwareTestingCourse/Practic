package com.processing.cardmanagement.services;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LuhnValidatorTest {

    private final LuhnValidator luhnValidator = new LuhnValidator();

    @ParameterizedTest
    @ValueSource(strings = {"400000", "400001", "400002", "400003", "400004"})
    void shouldGenerateValidPanWhenBinIsValid(String bin) {
        String pan = luhnValidator.generatePan(bin);

        assertThat(pan)
                .hasSize(16)
                .startsWith(bin)
                .matches("\\d{16}");
        assertThat(luhnValidator.isValid(pan)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "4000000000000002, true",
            "4000000000000003, false"
    })
    void shouldValidatePanWhenPanIsChecked(String pan, boolean expected) {
        assertThat(luhnValidator.isValid(pan)).isEqualTo(expected);
    }
}
