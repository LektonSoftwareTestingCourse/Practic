package com.processing.authorization.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;

import com.processing.authorization.repositories.LimitUsageRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Сущность для хранения накопленных сумм операций по карте за день и месяц.
 * <p>
 * Используется для контроля лимитов при авторизации транзакций.
 * Каждая запись относится к одной карте (PAN) на конкретную дату.
 * <p>
 * <b>Ключевые ограничения:</b>
 * <ul>
 * <li>Уникальность: одна запись на {@code (pan, usage_date)}</li>
 * <li>PAN — 16 цифр (соответствует стандарту ISO/IEC 7812)</li>
 * <li>Суммы хранятся с точностью до 2 знаков после запятой</li>
 * </ul>
 * <p>
 * Обновление полей {@code daily_amount} и {@code monthly_amount}
 * выполняется атомарно через {@link LimitUsageRepository#upsertLimitUsage}
 *
 * @see com.processing.authorization.repositories.LimitUsageRepository
 */
@Entity
@Table(name = "limit_usage",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"pan", "usage_date"}) },
        indexes = {
            @Index(name = "idx_limit_usage_card", columnList = "pan"),
            @Index(name = "idx_limit_usage_date", columnList = "usage_date")
        })
@Data
public class LimitUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 16)
    private String pan;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "daily_amount", nullable = false)
    @ColumnDefault("0")
    private BigDecimal dailyAmount;

    @Column(name = "monthly_amount", nullable = false)
    @ColumnDefault("0")
    private BigDecimal monthlyAmount;

    @Column(name = "updated_at")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @UpdateTimestamp
    private Instant updatedAt;
}
