package com.processing.config;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Фабрика экземпляров Resilience4j {@link Retry} для Authorization и Logger.
 */
public final class RetryFactory {

    private static final long MIN_INTERVAL_MS = 1;

    private static final Logger LOG = LoggerFactory.getLogger(RetryFactory.class);

    private RetryFactory() {
    }

    /**
     * Создаёт retry-политику для вызовов Transaction Logger.
     *
     * @param properties конфигурация Switch
     * @return настроенный {@link Retry} с именем {@code "logger"}
     */
    public static Retry loggerRetry(SwitchProperties properties) {
        return createRetry("logger", properties);
    }

    /**
     * Создаёт retry-политику для вызовов Authorization Service.
     *
     * @param properties конфигурация Switch
     * @return настроенный {@link Retry} с именем {@code "authorization"}
     */
    public static Retry authorizationRetry(SwitchProperties properties) {
        return createRetry("authorization", properties);
    }

    /**
     * Собирает {@link Retry} с exponential backoff из конфигурации.
     *
     * @param name       имя retry для логирования
     * @param properties конфигурация Switch
     * @return экземпляр {@link Retry}
     */
    private static Retry createRetry(String name, SwitchProperties properties) {
        SwitchProperties.RetryProperties retry = properties.retry();
        IntervalFunction intervalFunction = intervalFunction(retry);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retry.maxAttempts())
                .intervalFunction(intervalFunction)
                .failAfterMaxAttempts(true)
                .build();

        Retry retryInstance = Retry.of(name, config);
        int maxAttempts = retry.maxAttempts();
        retryInstance.getEventPublisher()
                .onRetry(event -> LOG.warn(
                        "{} attempt {}/{} failed",
                        name,
                        event.getNumberOfRetryAttempts(),
                        maxAttempts,
                        event.getLastThrowable()))
                .onError(event -> LOG.error(
                        "{} unavailable after {} attempts",
                        name,
                        maxAttempts,
                        event.getLastThrowable()));

        return retryInstance;
    }

    /**
     * Строит функцию интервала между попытками из списка {@code backoffMs}.
     * Resilience4j передаёт {@code numberOfFailedAttempts} (1 после первой ошибки),
     * поэтому для списка {@code [1000, 2000, 4000]} паузы: 1s → 2s → 4s.
     *
     * @param retry настройки retry
     * @return функция задержки; при пустом списке — минимальный интервал 1 мс
     */
    private static IntervalFunction intervalFunction(SwitchProperties.RetryProperties retry) {
        List<Long> backoffMs = retry.backoffMs();
        if (backoffMs == null || backoffMs.isEmpty()) {
            return IntervalFunction.of(MIN_INTERVAL_MS);
        }
        return failedAttempts -> Math.max(
                MIN_INTERVAL_MS,
                backoffMs.get(Math.min(Math.max(failedAttempts, 1) - 1, backoffMs.size() - 1)));
    }
}
