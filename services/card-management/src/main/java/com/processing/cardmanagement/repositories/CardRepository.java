package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.exceptions.MassiveCardCreationCollisionException;
import com.processing.cardmanagement.exceptions.PanCollisionException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardStatus;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CardRepository {

    Optional<Card> findByPan(String pan);

    /**
     * Находит карту по номеру PAN и блокирует ее параллельное изменение
     *
     * @param pan номер карты
     * @return найденная карта
     */
    Optional<Card> findByPanForUpdate(String pan);

    /**
     * Возвращает список карт с фильтрацией и пагинацией
     * Параметры фильтрации опциональны, если передается null - фильтр по этому полю не применяется
     *
     * @return список карт
     */
    List<Card> findCards(
        int limit,
        long offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
    );

    /**
     * Возвращает список карт по списку PAN для изменения
     *
     * @return список карт
     */
    List<Card> findCardsByPansForUpdate(List<String> pans);

    /**
     * Возвращает список карт по списку BIN для изменения
     *
     * @return список карт
     */
    List<Card> findCardsByBinsForUpdate(List<String> bins);

    /**
     * Возвращает количество карт с применением фильтров
     *
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
     * @return количество хранящихся карт
     */
    long countAllCards();

    /**
     * Создает карту
     *
     * @param card сохраняемая карта
     * @return сохраненная карта
     * @throws PanCollisionException при попытке создать существующую карту
     */
    Card create(Card card);

    /**
     * Обновляет карту
     *
     * @param card сохраняемая карта
     * @return сохраненная карта
     * @throws PanCollisionException при попытке создать существующую карту
     */
    Card update(Card card);

    /**
     * Создает разом множество карт
     *
     * @param cards сохраняемые карты
     * @return сохраненные карты
     * @throws MassiveCardCreationCollisionException при попытке создать существующую карту
     */
    List<Card> createAll(List<Card> cards);
}
