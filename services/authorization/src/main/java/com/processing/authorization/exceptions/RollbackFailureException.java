package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при технической ошибке отката транзакции.
 * <p>
 * Возникает из-за системных проблем:
 * <ul>
 * <li>Таймаут при выполнении отката</li>
 * <li>Ошибка соединения с CMS</li>
 * <li>Ошибка целостности данных</li>
 * </ul>
 *
 */
public class RollbackFailureException extends RuntimeException {
    public RollbackFailureException(String message) {
        super(message);
    }
}
