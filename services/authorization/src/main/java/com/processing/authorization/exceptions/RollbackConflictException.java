package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при конфликте отката транзакции.
 * <p>
 * Возникает в следующих случаях:
 * <ul>
 * <li>Транзакция уже была откатана ранее</li>
 * <li>Транзакция находится в недопустимом для отката состоянии</li>
 * </ul>
 *
 * @see RollbackFailureException
 */
public class RollbackConflictException extends RuntimeException {
    public RollbackConflictException(String message) {
        super(message);
    }
}
