package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при ошибке получения данных карты из CMS.
 * <p>
 * Может быть вызвано как техническими проблемами (таймаут, ошибка сети),
 * так и логическими (карта не найдена, заблокирована).
 *
 */
public class GetCardException extends RuntimeException {
    public GetCardException(String message) {
        super(message);
    }
}
