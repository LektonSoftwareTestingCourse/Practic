package com.processing.transactionlogger.service;

import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.common.dto.transactionlogger.TransactionStoredResponse;

/**
 * Результат операции сохранения транзакции.
 * Ровно одно из полей ненулевое: {@code storedTransaction} при новой записи,
 * {@code existingTransaction} при повторном запросе с тем же {@code id}.
 */
public record TransactionStoreResult(
        TransactionResponse existingTransaction,
        TransactionStoredResponse storedTransaction
) {
    /** @return результат для уже существующей транзакции */
    public static TransactionStoreResult existing(TransactionResponse response) {
        return new TransactionStoreResult(response, null);
    }

    /** @return результат для только что созданной транзакции */
    public static TransactionStoreResult created(TransactionStoredResponse response) {
        return new TransactionStoreResult(null, response);
    }

    /** @return {@code true} если транзакция была создана в этом запросе */
    public boolean created() {
        return storedTransaction != null;
    }
}
