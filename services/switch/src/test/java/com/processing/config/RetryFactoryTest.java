package com.processing.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetryFactoryTest {
    @Test
    void shouldUseMinimumIntervalWhenBackoffListIsNull() {
        SwitchProperties properties = properties(null);

        long interval = RetryFactory.authorizationRetry(properties)
                .getRetryConfig().getIntervalFunction().apply(1);

        assertEquals(Long.valueOf(1L), interval);
    }

    @Test
    void shouldUseMinimumIntervalWhenBackoffListIsEmpty() {
        SwitchProperties properties = properties(List.of());

        long interval = RetryFactory.loggerRetry(properties)
                .getRetryConfig().getIntervalFunction().apply(1);

        assertEquals(Long.valueOf(1L), interval);
    }

    @Test
    void shouldUseConfiguredBackoffAndReuseLastValueForFurtherAttempts() {
        SwitchProperties properties = properties(List.of(100L, 250L));
        var intervalFunction = RetryFactory.authorizationRetry(properties)
                .getRetryConfig().getIntervalFunction();

        assertEquals(Long.valueOf(100L), intervalFunction.apply(1));
        assertEquals(Long.valueOf(250L), intervalFunction.apply(2));
        assertEquals(Long.valueOf(250L), intervalFunction.apply(3));
    }

    private static SwitchProperties properties(List<Long> backoff) {
        return new SwitchProperties("test", Map.of(), "http://auth", "http://logger", "http://merchant",
                new SwitchProperties.HttpProperties(100, 100, 100),
                new SwitchProperties.RetryProperties(3, backoff),
                new SwitchProperties.CircuitBreakerSection(
                        new SwitchProperties.CircuitBreakerProperties(50, 10, 5, 1000)));
    }
}
