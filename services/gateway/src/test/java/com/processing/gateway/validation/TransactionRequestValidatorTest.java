package com.processing.gateway.validation;

import com.processing.common.dto.authorization.AuthorizationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionRequestValidatorTest {

    private final TransactionRequestValidator validator = new TransactionRequestValidator();

    @Test
    void shouldAcceptValidAuthorizationRequest() {
        // Arrange
        AuthorizationRequest request = validRequest();

        // Act and Assert
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void shouldRejectMissingRequestBody() {
        // Act and Assert
        assertThrows(TransactionValidationException.class, () -> validator.validate(null));
    }

    @ParameterizedTest(name = "invalid request {index}")
    @MethodSource("invalidRequests")
    void shouldRejectInvalidEquivalenceClasses(AuthorizationRequest request) {
        // Act and Assert
        assertThrows(TransactionValidationException.class, () -> validator.validate(request));
    }

    private static Stream<AuthorizationRequest> invalidRequests() {
        AuthorizationRequest valid = validRequest();
        return Stream.of(
                copy(valid, "0200", valid.pan(), valid.amount(), valid.currencyCode(), valid.mcc()),
                copy(valid, valid.mti(), "400000123", valid.amount(), valid.currencyCode(), valid.mcc()),
                copy(valid, valid.mti(), valid.pan(), null, valid.currencyCode(), valid.mcc()),
                copy(valid, valid.mti(), valid.pan(), BigDecimal.ZERO, valid.currencyCode(), valid.mcc()),
                copy(valid, valid.mti(), valid.pan(), valid.amount(), "64", valid.mcc()),
                copy(valid, valid.mti(), valid.pan(), valid.amount(), valid.currencyCode(), "50A5"));
    }

    private static AuthorizationRequest copy(AuthorizationRequest source, String mti, String pan,
                                             BigDecimal amount, String currencyCode, String mcc) {
        return new AuthorizationRequest(
                mti, source.stan(), pan, source.processingCode(), amount, currencyCode,
                source.transmissionDateTime(), source.terminalId(), source.terminalType(),
                source.merchantId(), mcc, source.acquirerId(), source.issuerId());
    }

    private static AuthorizationRequest validRequest() {
        return new AuthorizationRequest(
                "0100", "000001", "4000001234567890", "000000", new BigDecimal("1000"),
                "643", Instant.parse("2026-10-01T10:15:30Z"), "TERM0001", "POS",
                "MERCH0000000002", "5045", "ACQ002", "ISS001");
    }
}
