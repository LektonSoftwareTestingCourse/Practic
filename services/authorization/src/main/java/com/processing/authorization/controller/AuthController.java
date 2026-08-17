package com.processing.authorization.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;

import com.processing.authorization.services.AuthServiceImpl;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;

/**
 * REST-контроллер для обработки запросов на авторизацию банковских карт.
 * <p>
 * Предоставляет эндпоинт для выполнения авторизации транзакций по картам.
 * Контроллер принимает запросы на авторизацию, делегирует бизнес-логику
 * сервису {@link AuthServiceImpl} и формирует HTTP-ответ с соответствующим
 * статус-кодом в зависимости от результата авторизации.
 * </p>
 * <p>
 * Все эндпоинты доступны по базовому пути {@code /api/internal}.
 * </p>
 *
 * @author core-auth-team
 * @see AuthServiceImpl
 * @see AuthorizationRequest
 * @see AuthorizationResponse
 */
public interface AuthController {
    /**
     * Обрабатывает запрос на авторизацию банковской карты.
     *
     * <p>
     * Метод выполняет следующие действия:
     * </p>
     * <ol>
     * <li>Фиксирует время начала обработки запроса</li>
     * <li>Делегирует авторизацию сервису
     * {@link AuthServiceImpl#authorize(AuthorizationRequest, Instant)}</li>
     * <li>Вычисляет время обработки запроса в миллисекундах</li>
     * <li>Устанавливает время обработки в ответе</li>
     * <li>Определяет HTTP-статус на основе результата авторизации</li>
     * <li>Возвращает ResponseEntity с соответствующим статусом и телом ответа</li>
     * </ol>
     *
     * <p>
     * В случае нарушения ограничений, заданных в {@link AuthorizationRequest},
     * будет возвращен ответ с ошибкой валидации.
     * </p>
     *
     * @param request запрос на авторизацию, содержащий:
     *                <ul>
     *                <li><b>pan</b> - номер карты (16 цифр)</li>
     *                <li><b>amount</b> - сумма транзакции</li>
     *                <li>другие параметры транзакции</li>
     *                </ul>
     * @return {@link ResponseEntity} с объектом {@link AuthorizationResponse},
     *         содержащим:
     *         <ul>
     *         <li><b>status</b> - статус авторизации (APPROVED или DECLINED)</li>
     *         <li><b>declineReason</b> - причина отклонения (если статус
     *         DECLINED)</li>
     *         <li><b>responseCode</b> - код ответа (двузначный ISO-код)</li>
     *         <li><b>rrn</b> - Retrieval Reference Number (при успешной
     *         авторизации)</li>
     *         <li><b>authCode</b> - код авторизации (при успешной авторизации)</li>
     *         <li><b>processingTimeMs</b> - время обработки запроса в
     *         12:59
     *         миллисекундах</li>
     *         </ul>
     *         HTTP-статус зависит от результата.
     *
     * @see AuthServiceImpl#authorize(AuthorizationRequest, Instant)
     * @see AuthorizationRequest
     * @see AuthorizationResponse
     */
    ResponseEntity<AuthorizationResponse> authorize(AuthorizationRequest request);

    /**
     * Обрабатывает запрос на откат ранее выполненной транзакции.
     * <p>
     * Метод выполняет следующие действия:
     * </p>
     * <ol>
     * <li>Фиксирует время начала обработки запроса</li>
     * <li>Делегирует откат сервису
     * {@link AuthServiceImpl#rollback(RollbackRequest, Instant)}</li>
     * <li>Определяет HTTP-статус на основе результата отката</li>
     * <li>Возвращает {@link ResponseEntity} с соответствующим статусом и телом
     * ответа</li>
     * </ol>
     * <p>
     * В случае нарушения ограничений, заданных в {@link RollbackRequest},
     * будет возвращён ответ с ошибкой валидации (HTTP 400).
     * </p>
     *
     * @param request запрос на откат транзакции, содержащий:
     *                <ul>
     *                <li><b>pan</b> — номер карты (16 цифр)</li>
     *                <li><b>amount</b> — сумма отката</li>
     *                <li><b>rrn</b> — RRN исходной транзакции (обязательно)</li>
     *                </ul>
     * @return {@link ResponseEntity} с объектом {@link RollbackResponse},
     *         содержащим:
     *         <ul>
     *         <li><b>status</b> — статус отката ("approved" или "declined")</li>
     *         <li><b>declineReason</b> — причина отклонения (если статус
     *         "declined")</li>
     *         <li><b>responseCode</b> — код ответа (двузначный ISO-код)</li>
     *         <li><b>rrn</b> — новый RRN отката (при успешном откате)</li>
     *         <li><b>processingTimeMs</b> — время обработки запроса в
     *         миллисекундах</li>
     *         </ul>
     *         HTTP-статус определяется на основе результата:
     *         <ul>
     *         <li><b>200 OK</b> — откат выполнен успешно</li>
     *         <li><b>404 Not Found</b> — транзакция или карта не найдены</li>
     *         <li><b>409 Conflict</b> — транзакция уже была откатана</li>
     *         <li><b>503 Service Unavailable</b> — сервис управления картами
     *         недоступен</li>
     *         <li><b>400 Bad Request</b> — некорректный запрос или неизвестная
     *         ошибка</li>
     *         </ul>
     * @see AuthServiceImpl#rollback(RollbackRequest, Instant)
     * @see RollbackRequest
     * @see RollbackResponse
     */
    ResponseEntity<RollbackResponse> rollback(RollbackRequest request);
}
