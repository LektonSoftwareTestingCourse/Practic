package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при недоступности внешнего сервиса.
 * <p>
 * Возникает в следующих случаях:
 * <ul>
 * <li>Таймаут соединения с сервисом</li>
 * <li>Сервис не отвечает (HTTP 5xx, 503)</li>
 * <li>Сетевые проблемы</li>
 * </ul>
 *
 * @see InternalCardManagerException
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
