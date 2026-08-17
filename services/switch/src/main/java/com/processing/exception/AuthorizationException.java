package com.processing.exception;

/**
 * Исключение при недоступности Authorization Service после исчерпания retry.
 */
public class AuthorizationException extends RuntimeException {

    private final String stan;

    /**
     * @param stan      STAN транзакции
     * @param attempts  число выполненных попыток
     * @param lastError сообщение последней ошибки
     */
    public AuthorizationException(String stan, int attempts, String lastError) {
        super(String.format(
                "Authorization service unavailable for STAN=%s after %d attempts. Last error: %s",
                stan, attempts, lastError != null ? lastError : "unknown"));
        this.stan = stan;
    }

    /**
     * @return STAN транзакции, для которой не удалось вызвать Authorization
     */
    public String getStan() {
        return stan;
    }

}
