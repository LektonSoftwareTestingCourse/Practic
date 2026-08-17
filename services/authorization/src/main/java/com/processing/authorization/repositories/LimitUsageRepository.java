package com.processing.authorization.repositories;

import com.processing.authorization.entities.LimitUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Репозиторий для работы с таблицей limit_usage, хранящей накопленные суммы
 * операций по картам за день и месяц.
 * <p>
 * Используется для контроля лимитов при авторизации транзакций.
 *
 * @see JpaRepository
 */
@Repository
public interface LimitUsageRepository extends JpaRepository<LimitUsage, UUID> {
        /**
         * Удаляет записи об использовании лимитов за указанный период.
         * <p>
         * Используется в задаче очистки устаревших данных для предотвращения
         * неограниченного роста таблицы.
         *
         * @param startDate начальная дата периода включительно
         * @param endDate   конечная дата периода включительно
         * @return количество удалённых записей
         */
        int deleteByUsageDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Атомарно обновляет или вставляет запись об использовании лимитов для карты.
         * <p>
         * Операция выполняется на уровне БД с использованием
         * {@code INSERT ... ON CONFLICT}:
         * <ul>
         * <li>Если записи на дату нет — вставляется новая с накопленными суммами</li>
         * <li>Если запись уже существует — сумма увеличивается</li>
         * </ul>
         * <p>
         * <b>Важно:</b> Обновление выполняется только при соблюдении обоих лимитов
         * (дневного и месячного) как для вставки, так и для обновления.
         * <p>
         * Месячная сумма для новой записи вычисляется как сумма последнего значения
         * за текущий месяц + текущая сумма операции.
         *
         * @param pan          PAN карты (идентификатор клиента)
         * @param date         дата операции (используется как ключ для дневного лимита)
         * @param amount       сумма операции для добавления к накопленным значениям
         * @param dailyLimit   максимально допустимая сумма операций за день
         * @param monthlyLimit максимально допустимая сумма операций за месяц
         * @return количество затронутых строк (0 — лимит превышен, 1 — успешно)
         */
        @Modifying
        @Query(value =
                "INSERT INTO limit_usage (id, pan, usage_date, daily_amount, monthly_amount) "
                + "SELECT gen_random_uuid(), :pan, :date, :amount, "
                + "       COALESCE((SELECT lu.monthly_amount "
                + "                 FROM limit_usage lu "
                + "                 WHERE lu.pan = :pan "
                + "                 AND lu.usage_date = (SELECT MAX(lu2.usage_date) "
                + "                                      FROM limit_usage lu2 "
                + "                                      WHERE lu2.pan = :pan "
                + "                                      AND DATE_TRUNC('month', lu2.usage_date) "
                + "                                      = DATE_TRUNC('month', CAST(:date AS date)))), 0) + :amount "
                + "WHERE :amount <= :dailyLimit "
                + "AND COALESCE((SELECT lu.monthly_amount "
                + "              FROM limit_usage lu "
                + "              WHERE lu.pan = :pan "
                + "              AND lu.usage_date = (SELECT MAX(lu2.usage_date) "
                + "                                   FROM limit_usage lu2 "
                + "                                   WHERE lu2.pan = :pan "
                + "                                   AND DATE_TRUNC('month', lu2.usage_date) "
                + "                                   = DATE_TRUNC('month', CAST(:date AS date)))), 0) "
                + "                                   + :amount <= :monthlyLimit "
                + "ON CONFLICT (pan, usage_date) DO UPDATE SET "
                + "daily_amount = limit_usage.daily_amount + :amount, "
                + "monthly_amount = limit_usage.monthly_amount + :amount "
                + "WHERE limit_usage.daily_amount + :amount <= :dailyLimit "
                + "AND limit_usage.monthly_amount + :amount <= :monthlyLimit", nativeQuery = true
        )
        int upsertLimitUsage(@Param("pan") String pan,
                        @Param("date") LocalDate date,
                        @Param("amount") BigDecimal amount,
                        @Param("dailyLimit") BigDecimal dailyLimit,
                        @Param("monthlyLimit") BigDecimal monthlyLimit);

        /**
         * Получает следующее значение из последовательности rrn_seq.
         *
         * @return следующее значение последовательности
         */
        @Query(value = "SELECT nextval('rrn_seq')", nativeQuery = true)
        long getNextRrn();

        /**
         * Устанавливает текущее значение последовательности rrn_seq.
         * <p>
         * Используется для тестирования или восстановления после ошибок.
         *
         * @param newRrn новое значение последовательности
         */
        @Query(value = "SELECT setval('rrn_seq', :newRrn)", nativeQuery = true)
        void saveRrn(@Param("newRrn") long newRrn);

        /**
         * Атомарно получает и резервирует значение rrn.
         *
         */
        @Query(value = "SELECT setval('rrn_seq', nextval('rrn_seq'))", nativeQuery = true)
        long fetchRrnBlock();
}
