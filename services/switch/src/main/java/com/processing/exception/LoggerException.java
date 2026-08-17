package com.processing.exception;

import java.util.UUID;

/**
 * Исключение при недоступности Transaction Logger после исчерпания retry.
 */
public class LoggerException extends RuntimeException {

    private final String stan;

    /**
     * @param stan          STAN транзакции
     * @param transactionId UUID транзакции, сгенерированный Switch
     * @param attempts      число выполненных попыток
     */
    public LoggerException(String stan, UUID transactionId, int attempts) {
        super(String.format(
                "Logger unavailable for TX %s (id=%s) after %d attempts",
                stan, transactionId, attempts));
        this.stan = stan;
    }

    /**
     * @return STAN транзакции, которую не удалось записать в Logger
     */
    public String getStan() {
        return stan;
    }

}
