package com.processing.authorization.client;

import com.processing.authorization.exceptions.*;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.ReserveRequest;

import java.math.BigDecimal;

/**
 * Клиент для взаимодействия с сервисом Card Management.
 * <p>
 * Предоставляет методы для выполнения операций с картами:
 * <ul>
 *  <li>Получение информации о карте</li>
 *  <li>Резервирование средств</li>
 *  <li>Откат транзакций</li>
 * </ul>
 * <p>
 * Все методы могут выбрасывать {@link ServiceUnavailableException} при недоступности Card Management
 * или {@link InternalCardManagerException} при внутренних ошибках сервиса Card Management.
 * </p>
 *
 * @author core-auth-team
 * @see CardModel
 * @see ReserveRequest
 * @see RollbackRequest
 */
public interface CardManagementClient {
    /**
     * Получает информацию о карте из Card Management по номеру PAN.
     *
     * <p>
     * Выполняет GET-запрос к Card Management на эндпоинт {@code /api/cards/{pan}}.
     * Обрабатывает различные HTTP-статусы ответа.
     * </p>
     *
     * @param pan номер карты (Primary Account Number) - 16-значный номер
     * @return {@link CardModel} с полной информацией о карте:
     * статус, срок действия, доступный баланс и другие атрибуты
     *
     * @throws CardNotFoundException если карта с указанным PAN не найдена (HTTP 404)
     * @throws ServiceUnavailableException если Card Manager недоступен (HTTP 503)
     * @throws InternalCardManagerException если произошла внутренняя ошибка Card Manager (HTTP 500)
     * @throws InvalidGetCardRequestException если передан некорректный PAN (HTTP 400)
     * @throws PaymentRequiredException если карта требует оплаты (HTTP 402)
     * @throws GetCardException если произошла другая ошибка при получении карты (не 2xx статус)
     *
     * @see CardModel
     * @see CardNotFoundException
     * @see ServiceUnavailableException
     */
    CardModel getCard(String pan);

    /**
     * Резервирует указанную сумму на карте через Card Management.
     *
     * <p>
     * Выполняет POST-запрос к Card Manager на эндпоинт {@code /api/cards/{pan}/reserve}
     * с телом запроса, содержащим сумму резервирования и RRN транзакции.
     * Резервирование необходимо для блокировки средств на карте до момента
     * фактического списания.
     * </p>
     *
     * @param amount сумма для резервирования в минимальных единицах валюты (копейки, центы)
     * @param rrn уникальный идентификатор транзакции (Retrieval Reference Number)
     * @param pan номер карты для резервирования средств
     *
     * @throws CardNotFoundException если карта не найдена (HTTP 404)
     * @throws ServiceUnavailableException если CMS недоступен (HTTP 503)
     * @throws InternalCardManagerException если произошла внутренняя ошибка CMS (HTTP 500)
     * @throws InvalidReserveRequestException если запрос на резервирование некорректен (HTTP 400)
     * @throws InsufficientFundsException если на карте недостаточно средств (HTTP 402)
     * @throws ReserveException если произошла другая ошибка при резервировании (не 2xx статус)
     *
     * @see ReserveRequest
     * @see ReserveException
     */
    void reserve(BigDecimal amount, String rrn, String pan);

    /**
     * Выполняет откат ранее зарезервированной транзакции.
     * <p>
     * Выполняет POST-запрос к Card Manager на эндпоинт {@code /api/cards/{pan}/rollback}
     * для освобождения зарезервированных средств.
     * Откат должен выполняться, если транзакция отклонена на следующих этапах обработки.
     * </p>
     *
     * @param request запрос на откат, содержащий:
     * <ul>
     *   <li><b>rrn</b> - идентификатор транзакции</li>
     *   <li><b>pan</b> - номер карты</li>
     *   <li><b>amount</b> - сумма для отката</li>
     * </ul>
     * @throws CardNotFoundException если карта не найдена (HTTP 404)
     * @throws ServiceUnavailableException если CMS недоступен (HTTP 503)
     * @throws InternalCardManagerException если произошла внутренняя ошибка CMS (HTTP 500)
     * @throws InvalidRollbackRequestException если запрос на откат некорректен (HTTP 400)
     * @throws RollbackConflictException если транзакция уже была откатана (HTTP 409)
     * @throws RollbackFailureException если произошла другая ошибка при откате (не 2xx статус)
     *
     * @see RollbackRequest
     * @see #reserve(BigDecimal, String, String)
     */
    void rollback(RollbackRequest request);
}
