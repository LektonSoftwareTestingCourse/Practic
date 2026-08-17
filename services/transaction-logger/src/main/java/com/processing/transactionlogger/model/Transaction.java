package com.processing.transactionlogger.model;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA-сущность транзакции — основная единица хранения в таблице {@code transactions}.
 * Создаётся при приёме запроса от Switch, никогда не изменяется после сохранения.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_status", columnList = "status"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at"),
        @Index(name = "idx_transactions_pan", columnList = "pan"),
        @Index(name = "idx_transactions_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class Transaction {

    @Id
    @Column(nullable = false)
    private UUID id;

    /** Message Type Indicator — тип сообщения (например, {@code 0110} — ответ на авторизацию) */
    @Column(nullable = false, length = 4)
    private String mti;

    /** System Trace Audit Number — порядковый номер транзакции в рамках сессии терминала */
    @Column(nullable = false, length = 6)
    private String stan;

    /** Retrieval Reference Number — уникальный номер транзакции, присвоенный Authorization Service */
    @Column(length = 12)
    private String rrn;

    /** Номер карты (Primary Account Number) */
    @Column(nullable = false, length = 16)
    private String pan;

    /** Код операции по ISO 8583 (например, {@code 000000} — покупка) */
    @Column(nullable = false, length = 6)
    private String processingCode;

    /** Сумма транзакции в копейках/центах (например, 150 руб. → 15000) */
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 8)
    private String terminalId;

    @Column(length = 10)
    private String terminalType;

    @Column(nullable = false, length = 15)
    private String merchantId;

    /** Merchant Category Code — код категории торговой точки */
    @Column(nullable = false, length = 4)
    private String mcc;

    @Column(nullable = false, length = 10)
    private String acquirerId;

    @Column(length = 10)
    private String issuerId;

    /** Комиссия эквайера в минорных единицах валюты */
    private BigDecimal acquiringFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /** Причина отклонения — заполняется только при {@code status = DECLINED} */
    @Column(length = 100)
    private String declineReason;

    /** Код авторизации — заполняется только при {@code status = APPROVED} */
    @Column(length = 6)
    private String authCode;

    /** Время обработки транзакции в миллисекундах (от приёма до ответа Authorization Service) */
    private Integer processingTimeMs;

    /** Время отправки транзакции с терминала */
    @Column(nullable = false)
    private Instant transmissionDateTime;

    /** Время сохранения записи в БД логгера */
    @Column(nullable = false)
    private Instant createdAt;
}
