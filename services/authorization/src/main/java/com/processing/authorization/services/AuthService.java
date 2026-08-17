package com.processing.authorization.services;

import java.time.Instant;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.cardmanagement.CardModel;

/**
 * Сервис авторизации транзакций по банковским картам.
 * <p>
 * Выполняет полный цикл авторизации карточной транзакции, включающий:
 * </p>
 * <ol>
 * <li>Получение данных карты из Card Management System (CMS)</li>
 * <li>Проверку статуса карты (ACTIVE, BLOCKED, INACTIVE, EXPIRED)</li>
 * <li>Проверку срока действия карты</li>
 * <li>Проверку доступного баланса</li>
 * <li>Резервирование средств на карте</li>
 * <li>Генерацию уникальных идентификаторов транзакции (RRN и AuthCode)</li>
 * </ol>
 * <p>
 * В случае любой ошибки на любом из этапов возвращается declined-ответ
 * с соответствующим кодом причины отклонения.
 * </p>
 *
 * @author core-auth-team
 * @see AuthorizationRequest
 * @see AuthorizationResponse
 * @see CardModel
 */
public interface AuthService {
    /**
     * Выполняет авторизацию транзакции по банковской карте.
     *
     * @param request запрос на авторизацию, содержащий
     *               PAN карты, сумму и другие
     *               параметры
     * @return {@link AuthorizationResponse} с результатом авторизации:
     *         <ul>
     *         <li>При успехе: статус "approved", RRN и AuthCode</li>
     *         <li>При отказе: статус "declined", причина отказа и код ответа</li>
     *         </ul>
     *
     * @see #generateRRN()
     * @see #generateAuthCode()
     * @see AuthorizationRequest
     * @see AuthorizationResponse
     */
    AuthorizationResponse authorize(AuthorizationRequest request, Instant requestInputTime);

    /**
     * Выполняет откат транзакции по банковской карте.
     *
     * @param request запрос на откат транзакции, содержащий
     *               PAN карты, сумму и rrn
     * @return {@link RollbackResponse} с результатом отката:
     *         <ul>
     *         <li>При успехе: статус "approved", RRN</li>
     *         <li>При отказе: статус "declined", причина отказа и код ответа</li>
     *         </ul>
     *
     * @see RollbackRequest
     * @see RollbackResponse
     */
    RollbackResponse rollback(RollbackRequest request, Instant requestInputTime);

    /**
     * Генерирует уникальный RRN в формате: yyDDD + 8 цифр.
     * <p>
     * Формат: 12 цифр
     * <ul>
     *   <li>Позиции 1-4: yDDD (последняя цифра года и день года)</li>
     *   <li>Позиции 5-12: 8 цифр из последовательности</li>
     * </ul>
     *
     * @return уникальный RRN из 12 цифр
     */
    String generateRRN();

    /**
     * Генерирует случайный код авторизации (AuthCode) для транзакции.
     *
     * <p>
     * AuthCode представляет собой 6-символьную строку, состоящую из
     * случайных букв верхнего регистра (A-Z) и цифр (0-9). Используется
     * как дополнительный идентификатор одобренной транзакции.
     * </p>
     *
     * <p>
     * <b>Пример AuthCode:</b> "A3F9K2", "Z7M1X0"
     * </p>
     *
     * <p>
     * Алфавит генерации: 36 символов (0-9, A-Z)
     * </p>
     *
     * @return строка из 6 случайных символов (буквы и цифры)
     */
    String generateAuthCode();
}
