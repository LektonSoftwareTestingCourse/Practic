package com.processing.transactionlogger.specification;

import com.processing.transactionlogger.model.Transaction;
import com.processing.transactionlogger.model.Transaction_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Фабрика JPA Specification для динамической фильтрации транзакций.
 * Каждый предикат применяется только при наличии соответствующего значения в фильтре.
 */
public class TransactionSpecification {

    /**
     * Собирает составной предикат из параметров фильтра.
     * Отсутствующие параметры игнорируются (не добавляют условие в запрос).
     *
     * @param filter параметры фильтрации
     * @return Specification для передачи в репозиторий
     */
    public static Specification<Transaction> filter(TransactionFilter filter) {
        return Specification
                .where(equals(Transaction_.PAN, filter.getPan()))
                .and(equals(Transaction_.STATUS, filter.getStatus()))
                .and(dateFrom(filter.getDateFrom()))
                .and(dateTo(filter.getDateTo()))
                .and(equals(Transaction_.MERCHANT_ID, filter.getMerchantId()))
                .and(equals(Transaction_.ISSUER_ID, filter.getIssuerId()))
                .and(equals(Transaction_.MCC, filter.getMcc()))
                .and(fullTextDeclineReason(filter.getDeclineReason()));
    }

    private static Specification<Transaction> equals(String field, Object value) {
        return (root, query, criteriaBuilder) ->
                value == null ? null : criteriaBuilder.equal(root.get(field), value);
    }

    private static Specification<Transaction> dateFrom(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return null;
            }
            Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Transaction_.TRANSMISSION_DATE_TIME), from);
        };
    }

    private static Specification<Transaction> dateTo(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return null;
            }
            Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return criteriaBuilder.lessThan(root.get(Transaction_.TRANSMISSION_DATE_TIME), to);
        };
    }

    private static Specification<Transaction> fullTextDeclineReason(String declineReason) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(declineReason)) {
                return null;
            }
            return criteriaBuilder.isTrue(criteriaBuilder.function(
                    "fts_match_decline_reason",
                    Boolean.class,
                    root.get(Transaction_.DECLINE_REASON),
                    criteriaBuilder.literal(declineReason)
            ));
        };
    }
}
