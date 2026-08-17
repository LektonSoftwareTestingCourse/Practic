package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при некорректном запросе на резервирование средств.
 *
 */
public class InvalidReserveRequestException extends RuntimeException {
    public InvalidReserveRequestException(String message) {
        super(message);
    }
}
