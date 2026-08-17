package com.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;
import java.util.Map;

/**
 * Конфигурация Switch из {@code application.yml} (префикс {@code switch}).
 *
 * @param version              версия сервиса для health-check
 * @param binRouting           таблица BIN → issuerId
 * @param authorizationUrl     базовый URL Authorization Service
 * @param loggerUrl            базовый URL Transaction Logger
 * @param merchantAcquirerUrl  базовый URL Merchant Acquirer Simulator
 * @param http                 таймауты HTTP-клиентов
 * @param retry                настройки Resilience4j retry
 * @param circuitBreaker       настройки Resilience4j circuit breaker
 */
@ConfigurationProperties(prefix = "switch")
public record SwitchProperties(
        String version,
        Map<String, String> binRouting,
        String authorizationUrl,
        String loggerUrl,
        String merchantAcquirerUrl,
        HttpProperties http,
        RetryProperties retry,
        CircuitBreakerSection circuitBreaker
) {
    /**
     * Таймауты подключения и чтения для REST-клиентов.
     *
     * @param connectTimeoutMs   таймаут установки соединения (мс)
     * @param authReadTimeoutMs  таймаут чтения ответа Authorization (мс)
     * @param loggerReadTimeoutMs таймаут чтения ответа Logger (мс)
     */
    public record HttpProperties(
            int connectTimeoutMs,
            int authReadTimeoutMs,
            int loggerReadTimeoutMs
    ) {}

    /**
     * Параметры повторных попыток при сбоях downstream-сервисов.
     *
     * @param maxAttempts максимальное число попыток
     * @param backoffMs   интервалы между попытками (мс), exponential backoff
     */
    public record RetryProperties(
            int maxAttempts,
            List<Long> backoffMs
    ) {}

    /**
     * Секция circuit breaker в конфигурации Switch.
     *
     * @param authorization параметры CB для Authorization Service
     */
    public record CircuitBreakerSection(
            CircuitBreakerProperties authorization
    ) {}

    /**
     * Параметры circuit breaker для вызовов Authorization.
     *
     * @param failureRateThreshold       порог ошибок (%), при котором контур открывается
     * @param slidingWindowSize          размер скользящего окна вызовов
     * @param minimumNumberOfCalls       минимум вызовов перед расчётом failure rate
     * @param waitDurationInOpenStateMs  время в OPEN-состоянии (мс)
     */
    public record CircuitBreakerProperties(
            int failureRateThreshold,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            long waitDurationInOpenStateMs
    ) {}
}
