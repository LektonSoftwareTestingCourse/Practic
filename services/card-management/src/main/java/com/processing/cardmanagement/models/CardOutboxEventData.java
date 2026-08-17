package com.processing.cardmanagement.models;

import com.processing.cardmanagement.events.CardOutboxEvent;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Модель события в outbox
 *
 * @param id          уникальный идентификатор
 * @param event       событие которое нужно обработать
 * @param createdAt   время создания события
 * @param processedAt время обработки события
 * @param retryCount  количество попыток обработки
 * @param lastError   последняя ошибка пр иобработке
 * @param status      стстус события
 */
public record CardOutboxEventData(
        UUID id,
        CardOutboxEvent event,
        Instant createdAt,
        @Nullable Instant processedAt,
        int retryCount,
        @Nullable String lastError,
        OutboxEventDataStatus status
) {

    /**
     * Создает новое событие со статусом PENDING
     *
     * @param event событие для сохранения в outbox
     */
    public CardOutboxEventData(CardOutboxEvent event) {
        this(
                UUID.randomUUID(),
                event,
                Instant.now(),
                null,
                0,
                null,
                OutboxEventDataStatus.PENDING
        );
    }

    /**
     * Возвращает копию события со статусом PROCESSED и временем обработки
     *
     * @return обработанное событие
     */
    public CardOutboxEventData processed() {
        return new CardOutboxEventData(
                id,
                event,
                createdAt,
                Instant.now(),
                retryCount,
                lastError,
                OutboxEventDataStatus.PROCESSED
        );
    }

    /**
     * Возвращает копию события с увеличенным счетчиком попыток и текстом ошибки
     *
     * @param error текст ошибки при обработке
     * @return событие с увеличенным retry count
     */
    public CardOutboxEventData withRetry(String error) {
        return new CardOutboxEventData(
                id,
                event,
                createdAt,
                processedAt,
                retryCount + 1,
                error,
                status
        );
    }

    /**
     * Возвращает копию события со статусом FAILED
     * Вызывается когда исчерпаны все попытки обработки
     *
     * @return событие со статусом FAILED
     */
    public CardOutboxEventData failed() {
        return new CardOutboxEventData(
                id,
                event,
                createdAt,
                processedAt,
                retryCount,
                lastError,
                OutboxEventDataStatus.FAILED
        );
    }
}
