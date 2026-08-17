package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при невозможности зарезервировать средства на
 * карте.
 * <p>
 * Возникает в следующих случаях:
 * <ul>
 * <li>Недостаточно средств (см. {@link InsufficientFundsException})</li>
 * <li>Техническая ошибка при резервировании</li>
 * </ul>
 *
 * @see InsufficientFundsException
 * @see InternalCardManagerException
 */
public class ReserveException extends RuntimeException {
    public ReserveException(String message) {
        super(message);
    }
}
