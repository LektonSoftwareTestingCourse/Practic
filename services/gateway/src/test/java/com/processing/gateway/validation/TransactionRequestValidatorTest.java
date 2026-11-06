package com.processing.gateway.validation;

import com.processing.common.dto.authorization.AuthorizationRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRequestValidatorTest {

    private final TransactionRequestValidator validator = new TransactionRequestValidator();

    @Test
    void shouldAcceptRequestWhenRequestIsValid() {
        assertThatCode(() -> validator.validate(request().build()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectRequestWhenPanHasFifteenDigits() {
        AuthorizationRequest request = request()
                .pan("400000000000001")
                .build();

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'pan' must be exactly 16 digits");
    }

    @Test
    void shouldRejectRequestWhenMtiIsWrong() {
        AuthorizationRequest request = request()
                .mti("0200")
                .build();

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'mti' must be '0100'");
    }

    @Test
    void shouldRejectRequestWhenRequestIsNull() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Request body is required");
    }

    @Test
    void shouldRejectRequestWhenRequiredFieldIsMissing() {
        AuthorizationRequest request = request()
                .terminalId(" ")
                .build();

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'terminalId' is required");
    }

    @Test
    void shouldRejectRequestWhenRequiredFieldIsNull() {
        AuthorizationRequest request = request()
                .mcc(null)
                .build();

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'mcc' is required");
    }

    @Test
    void shouldRejectRequestWhenAmountIsNull() {
        AuthorizationRequest request = request()
                .amount(null)
                .build();

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'amount' is required");
    }

    @Test
    void shouldRejectRequestWhenAmountIsNotPositive() {
        AuthorizationRequest request = request()
                .amount(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'amount' must be > 0");
    }

    @Test
    void shouldRejectRequestWhenCurrencyCodeLengthIsWrong() {
        AuthorizationRequest request = request()
                .currencyCode("64")
                .build();

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'currencyCode' must be exactly 3 characters");
    }

    @Test
    void shouldRejectRequestWhenMccIsWrong() {
        AuthorizationRequest request = request()
                .mcc("50A5")
                .build();

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'mcc' must be exactly 4 digits");
    }

    private static AuthorizationRequest.AuthorizationRequestBuilder request() {
        return AuthorizationRequest.builder()
                .mti("0100")
                .stan("000001")
                .pan("4000000000000002")
                .processingCode("000000")
                .amount(new BigDecimal("1000"))
                .currencyCode("643")
                .transmissionDateTime(Instant.parse("2026-01-01T10:00:00Z"))
                .terminalId("TERM0001")
                .terminalType("POS")
                .merchantId("MERCH0000000002")
                .mcc("5045")
                .acquirerId("ACQ002")
                .issuerId("ISS001");
    }
}
