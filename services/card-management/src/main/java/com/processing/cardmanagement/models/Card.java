package com.processing.cardmanagement.models;

import com.processing.cardmanagement.exceptions.InsufficientFundsException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * Модель банковской карты
 *
 * @param id               уникальный идентификатор
 * @param pan              уникальный номер карты
 * @param bin              BIN номер карты
 * @param cardholderName   имя держателя карты
 * @param expiryDate       срок хранения карты
 * @param status           статус карты
 * @param currencyCode     код валюты
 * @param dailyLimit       дневной лимит карты
 * @param monthlyLimit     месячный лимит карты
 * @param availableBalance доступный баланс
 * @param issuerId         номер банка эмитента
 * @param createdAt        дата создания
 */
public record Card(
    UUID id,
    String pan,
    String bin,
    String cardholderName,
    YearMonth expiryDate,
    CardStatus status,
    String currencyCode,
    BigDecimal dailyLimit,
    BigDecimal monthlyLimit,
    BigDecimal availableBalance,
    String issuerId,
    Instant createdAt
) {

    public Card(
        UUID id,
        String pan,
        String bin,
        String cardholderName,
        YearMonth expiryDate,
        CardStatus status,
        String currencyCode,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        BigDecimal availableBalance,
        String issuerId
    ) {
        this(
            id,
            pan,
            bin,
            cardholderName,
            expiryDate,
            status,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance,
            issuerId,
            Instant.now()
        );
    }

    /**
     * Создает копию карты с новым ID и указанным PAN
     *
     * @param pan PAN-номер карты
     */
    public Card copyWithPan(
        String pan
    ) {
        return new Card(
            UUID.randomUUID(),
            pan,
            bin,
            cardholderName,
            expiryDate,
            status,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance,
            issuerId
        );
    }

    /**
     * Проверяет статус карты и после этого возвращает запрос на списание средств
     *
     * @param amount размер списания
     * @param rrn    RRN-номер
     * @return информация о списании
     */
    public Reservation startReservation(BigDecimal amount, String rrn) {
        checkStatusOrThrow();
        if (availableBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        return new Reservation(this.pan, amount, rrn);
    }

    /**
     * Создает копию карты с зарезервированным количеством средств
     *
     * @param reservation информация о резервировании
     * @return карта с измененным балансом
     */
    public Card withReservation(Reservation reservation) {
        if (!Objects.equals(this.pan, reservation.pan())) {
            throw new IllegalArgumentException("Reservation PAN number and card PAN number differ");
        }

        return new Card(
            id,
            pan,
            bin,
            cardholderName,
            expiryDate,
            status,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance.subtract(reservation.reservationAmount()),
            issuerId,
            createdAt
        );
    }

    /**
     * Создает копию карты с возвращенным количеством средств
     *
     * @param rollback информация о возврате средств
     * @return карта с измененным балансом
     */
    public Card withRollback(ReservationRollback rollback) {
        if (!Objects.equals(this.pan, rollback.pan())) {
            throw new IllegalArgumentException("Reservation PAN number and card PAN number differ");
        }

        return new Card(
            id,
            pan,
            bin,
            cardholderName,
            expiryDate,
            status,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance.add(rollback.rollbackAmount()),
            issuerId,
            createdAt
        );
    }

    /**
     * Ставит карте статус "Удалено"
     *
     * @return удаленная карта
     */
    public Card deleted() {
        if (this.status == CardStatus.DELETED) {
            throw new IllegalStateException("Card already deleted");
        }

        return new Card(
            id,
            pan,
            bin,
            cardholderName,
            expiryDate,
            CardStatus.DELETED,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance,
            issuerId,
            createdAt
        );
    }

    /**
     * Изменяет некоторые параметры у карты
     *
     * @param status           новый статус
     * @param dailyLimit       новый дневной лимит
     * @param monthlyLimit     новый месячный лимит
     * @param availableBalance новый баланс
     * @return измененная карта
     * @throws IllegalArgumentException отрицательный лимит или дневной лимит > месячного
     */
    public Card withData(
        CardStatus status,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        BigDecimal availableBalance
    ) {
        if (dailyLimit.compareTo(BigDecimal.ZERO) < 0 || monthlyLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Limit can not contain negative values");
        }
        if (monthlyLimit.compareTo(dailyLimit) < 0) {
            throw new IllegalArgumentException("Daily limit can not be larger than monthly limit");
        }

        return new Card(
            id,
            pan,
            bin,
            cardholderName,
            expiryDate,
            status,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance,
            issuerId,
            createdAt
        );
    }

    /**
     * Создает заполненную карту из черновика
     *
     * @param pan                номер карты
     * @param issuerId           номер банка эмитента
     * @param cardValidityPeriod срок действия карты (в годах)
     * @param draft              частично заполненная карта
     * @return созданная карта
     */
    public static Card fromDraft(
        String pan,
        String issuerId,
        int cardValidityPeriod,
        CardDraft draft
    ) {
        return new Card(
            UUID.randomUUID(),
            pan,
            draft.bin(),
            draft.cardholderName(),
            YearMonth.now().plusYears(cardValidityPeriod),
            draft.status(),
            draft.currencyCode(),
            draft.dailyLimit(),
            draft.monthlyLimit(),
            draft.initialBalance(),
            issuerId
        );
    }

    /**
     * Проверяет перед операцией, что карта активна
     */
    public void checkStatusOrThrow() {
        if (status != CardStatus.ACTIVE) {
            throw new IllegalStateException("Can't perform operation: card status is \"" + status.name() + "\"");
        }
    }
}
