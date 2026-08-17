package com.processing.transactionlogger.specification;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

/**
 * Параметры фильтрации и пагинации для поиска транзакций.
 * Все поля опциональны. Ненулевые поля объединяются через AND в {@link TransactionSpecification}.
 */
@Data
@ValidDateRange
public class TransactionFilter {
    private String pan;
    private TransactionStatus status;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String merchantId;
    private String issuerId;
    private String mcc;
    private String declineReason;
    @Positive
    @Max(value = 500, message = "limit must not exceed 500")
    private int limit = 50;
    @PositiveOrZero
    private int offset = 0;
}
