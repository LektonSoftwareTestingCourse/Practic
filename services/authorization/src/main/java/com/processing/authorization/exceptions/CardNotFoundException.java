package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при попытке выполнения операции с картой,
 * которая не найдена в системе управления картами (CMS).
 * <p>
 * Возникает в следующих случаях:
 * <ul>
 * <li>Карта не зарегистрирована в системе</li>
 * <li>PAN карты не существует</li>
 * <li>Передан некорректный или невалидный PAN</li>
 * </ul>
 *
 */
public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String message) {
        super(message);
    }
}
