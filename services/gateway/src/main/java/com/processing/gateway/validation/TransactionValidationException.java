package com.processing.gateway.validation;

/**
 * Exception thrown when a transaction request fails gateway validation.
 */
public class TransactionValidationException extends RuntimeException {
    /**
     * Creates a validation exception with a client-facing message.
     *
     * @param message validation error message
     */
    public TransactionValidationException(String message) {
        super(message);
    }
}
