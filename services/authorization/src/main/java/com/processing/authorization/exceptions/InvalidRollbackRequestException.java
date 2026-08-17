package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при некорректном запросе на откат транзакции.
 *
 * @see InvalidReserveRequestException
 * @see InvalidGetCardRequestException
 */
public class InvalidRollbackRequestException extends RuntimeException {
    public InvalidRollbackRequestException(String message) {
        super(message);
    }
}
