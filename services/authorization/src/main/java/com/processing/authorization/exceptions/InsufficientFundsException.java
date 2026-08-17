package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при недостатке средств на карте для выполнения
 * операции.
 * <p>
 * Возникает когда запрашиваемая сумма превышает доступный баланс карты
 * с учётом уже заблокированных средств.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
