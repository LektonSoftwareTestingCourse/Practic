package com.processing.gateway.validation;

import com.processing.common.dto.authorization.AuthorizationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionRequestValidatorTest {
    private final TransactionRequestValidator validator = new TransactionRequestValidator();

    @Test
    void shouldAcceptValidAuthorizationRequest() {
        assertDoesNotThrow(() -> validator.validate(validRequest()));
    }

    @Test
    void shouldRejectNullRequest() {
        assertThrows(TransactionValidationException.class, () -> validator.validate(null));
    }

    @Test
    void shouldRejectWrongMessageType() {
        assertThrows(TransactionValidationException.class,
                () -> validator.validate(request("0200", "4000001234567890", BigDecimal.ONE, "643", "5045")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"400000123456789", "40000012345678901", "400000123456789X"})
    void shouldRejectPanThatIsNotExactlySixteenDigits(String pan) {
        assertThrows(TransactionValidationException.class,
                () -> validator.validate(request("0100", pan, BigDecimal.ONE, "643", "5045")));
    }

    @Test
    void shouldRejectMissingAmount() {
        assertThrows(TransactionValidationException.class,
                () -> validator.validate(request("0100", "4000001234567890", null, "643", "5045")));
    }

    @Test
    void shouldRejectZeroOrNegativeAmount() {
        assertThrows(TransactionValidationException.class,
                () -> validator.validate(request("0100", "4000001234567890", BigDecimal.ZERO, "643", "5045")));
        assertThrows(TransactionValidationException.class,
                () -> validator.validate(request("0100", "4000001234567890", new BigDecimal("-1"), "643", "5045")));
    }

    @Test
    void shouldRejectCurrencyCodeWithWrongLength() {
        assertThrows(TransactionValidationException.class,
                () -> validator.validate(request("0100", "4000001234567890", BigDecimal.ONE, "64", "5045")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"504", "50455", "50A5"})
    void shouldRejectMccThatIsNotFourDigits(String mcc) {
        assertThrows(TransactionValidationException.class,
                () -> validator.validate(request("0100", "4000001234567890", BigDecimal.ONE, "643", mcc)));
    }

    private static AuthorizationRequest validRequest() {
        return request("0100", "4000001234567890", new BigDecimal("100.00"), "643", "5045");
    }

    private static AuthorizationRequest request(
            String mti, String pan, BigDecimal amount, String currency, String mcc) {
        return new AuthorizationRequest(mti, "123456", pan, "000000", amount, currency,
                Instant.parse("2026-11-12T10:00:00Z"), "TERM0001", "POS", "MERCH0000000002", mcc, "ACQ001", null);
    }
}
