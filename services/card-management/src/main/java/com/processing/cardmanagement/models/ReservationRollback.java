package com.processing.cardmanagement.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Информация о возврате средств за удержание
 *
 * @param id             идентификатор
 * @param reservationId  идентификатор связанного удержания средств
 * @param pan            номер карты
 * @param rollbackAmount количество возвращаемых средств
 * @param rrn            RRN-номер
 * @param createdAt      дата создания
 */
public record ReservationRollback(
    UUID id,
    UUID reservationId,
    String pan,
    BigDecimal rollbackAmount,
    String rrn,
    Instant createdAt
) {}
