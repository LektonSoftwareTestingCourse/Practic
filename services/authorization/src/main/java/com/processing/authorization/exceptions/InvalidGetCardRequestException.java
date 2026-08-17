package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при некорректном запросе на получение карты.
 * <p>
 * Причины возникновения:
 * <ul>
 * <li>Пустой или невалидный PAN</li>
 * <li>Отсутствие обязательных параметров</li>
 * <li>Неверный формат данных</li>
 * </ul>
 *
 */
public class InvalidGetCardRequestException extends RuntimeException {
    public InvalidGetCardRequestException(String message) {
        super(message);
    }
}
