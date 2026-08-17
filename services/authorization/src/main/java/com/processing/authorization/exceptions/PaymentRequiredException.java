package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при необходимости дополнительной оплаты.
 */
public class PaymentRequiredException extends RuntimeException {
    public PaymentRequiredException(String message) {
        super(message);
    }
}
