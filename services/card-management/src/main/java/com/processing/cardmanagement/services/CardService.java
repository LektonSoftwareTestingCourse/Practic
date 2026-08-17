package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.exceptions.ReservationAlreadyExistsException;
import com.processing.cardmanagement.exceptions.ReservationNotFoundException;
import com.processing.cardmanagement.exceptions.RollbackAlreadySatisfiedException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


public interface CardService {

    Card createCard(
            String bin,
            String cardholderName,
            String currencyCode,
            BigDecimal dailyLimit,
            BigDecimal monthlyLimit,
            BigDecimal initialBalance
    );

    List<Card> createCards(List<CardDraft> data);

    Card getCard(String pan);

    /**
     * Возвращает список карт с пагинацией и фильтрацией
     *
     * @param limit     количество карт на странице
     * @param offset    смещение
     * @param status    фильтр по статусу
     * @param bin       фильтр по bin
     * @param issuerId  фильтр по IssuerId
     * @param startDate начало диапазона дат
     * @param endDate   конец диапазона дат
     * @return список карт
     */
    List<Card> getCards(
            @Nullable Integer limit,
            @Nullable Long offset,
            @Nullable CardStatus status,
            @Nullable String bin,
            @Nullable String issuerId,
            @Nullable Instant startDate,
            @Nullable Instant endDate
    );

    /**
     * Частично обновляет параметры карты
     *
     * @param pan              PAN карты
     * @param status           новый статус карты
     * @param dailyLimit       новый дневной лимит карты
     * @param monthlyLimit     новый месячный лимит карты
     * @param availableBalance новый баланс карты
     * @return измененная карта
     * @throws CardNotFoundException если карта не найдена
     */
    Card patchCard(
            String pan,
            @Nullable CardStatus status,
            @Nullable BigDecimal dailyLimit,
            @Nullable BigDecimal monthlyLimit,
            @Nullable BigDecimal availableBalance
    );

    /**
     * Удаляет карту
     *
     * @param pan PAN карты
     * @throws CardNotFoundException если карта не найдена
     */
    void deleteCard(String pan);

    /**
     * Возвращает общее количество карт в базе данных
     *
     * @return количество карт
     */
    long countAllCardsAndUpdate();

    /**
     * Считает количество карт, удовлетворяющее фильтрам
     *
     * @param status    фильтр по статусу
     * @param bin       фильтр по bin
     * @param issuerId  фильтр по IssuerId
     * @param startDate начало диапазона дат
     * @param endDate   конец диапазона дат
     * @return количество карт
     */
    long countCardsFiltered(
            @Nullable CardStatus status,
            @Nullable String bin,
            @Nullable String issuerId,
            @Nullable Instant startDate,
            @Nullable Instant endDate
    );

    /**
     * Резервирует средства на карте, уменьшая доступный баланс
     *
     * @param pan    PAN карты
     * @param amount размер резервирования
     * @param rrn    номер rrn
     * @return измененная карта
     * @throws CardNotFoundException             если карта не найдена
     * @throws ReservationAlreadyExistsException если RRN уже есть в БД
     */
    Card reserve(String pan, BigDecimal amount, String rrn);

    /**
     * Возвращает средства на карту, увеличивая доступный баланс
     *
     * @param pan    PAN карты
     * @param amount размер резервирования
     * @param rrn    номер rrn
     * @return измененная карта
     * @throws CardNotFoundException             если карта не найдена
     * @throws RollbackAlreadySatisfiedException если возврат уже был запрошен
     * @throws ReservationNotFoundException      если не найдено зачисление с данным RRN
     */
    Card rollback(String pan, BigDecimal amount, String rrn);

    int bulkUpdateStatus(
            @Nullable List<String> bin,
            @Nullable List<String> pans,
            CardStatus status
    );
}
