package com.processing.gateway.validation;

import com.processing.common.dto.authorization.AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Validates the authorization request contract required by the gateway.
 */
@Component
public class TransactionRequestValidator {

    /**
     * Validates a transaction authorization request.
     *
     * @param request authorization request to validate
     * @throws TransactionValidationException when the request violates gateway rules
     */
    public void validate(AuthorizationRequest request) {
        if (request == null) {
            throw new TransactionValidationException("Request body is required");
        }

        requireValue(request.mti(), "mti");
        requireValue(request.stan(), "stan");
        requireValue(request.pan(), "pan");
        requireValue(request.processingCode(), "processingCode");
        requireValue(request.currencyCode(), "currencyCode");
        requireValue(request.transmissionDateTime(), "transmissionDateTime");
        requireValue(request.terminalId(), "terminalId");
        requireValue(request.merchantId(), "merchantId");
        requireValue(request.mcc(), "mcc");
        requireValue(request.acquirerId(), "acquirerId");

        if (!"0100".equals(request.mti())) {
            throw new TransactionValidationException("Field 'mti' must be '0100'");
        }
        if (!request.pan().matches("\\d{16}")) {
            throw new TransactionValidationException("Field 'pan' must be exactly 16 digits");
        }
        if (request.amount() == null) {
            throw new TransactionValidationException("Field 'amount' is required");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionValidationException("Field 'amount' must be > 0");
        }
        if (request.currencyCode().length() != 3) {
            throw new TransactionValidationException("Field 'currencyCode' must be exactly 3 characters");
        }
        if (!request.mcc().matches("\\d{4}")) {
            throw new TransactionValidationException("Field 'mcc' must be exactly 4 digits");
        }
    }

    private void requireValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new TransactionValidationException("Field '" + fieldName + "' is required");
        }
    }

    private void requireValue(Instant value, String fieldName) {
        if (value == null) {
            throw new TransactionValidationException("Field '" + fieldName + "' is required");
        }
    }

}
