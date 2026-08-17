package com.processing.transactionlogger.exception;

import java.util.UUID;

/**
 * Бросается, когда транзакция с указанным {@code id} уже существует в БД,
 * но её данные не совпадают с данными из запроса.
 */
public class TransactionConflictException extends RuntimeException {

    /**
     * @param id UUID конфликтующей транзакции
     */
    public TransactionConflictException(UUID id) {
        super("Transaction with id " + id + " already exists with different data");
    }
}
