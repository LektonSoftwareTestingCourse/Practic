package com.processing.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Фабрика экземпляров Resilience4j {@link CircuitBreaker} для Authorization.
 */
public final class CircuitBreakerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerFactory.class);

    private CircuitBreakerFactory() {
    }

    /**
     * Создаёт circuit breaker для вызовов Authorization Service.
     *
     * @param properties конфигурация Switch
     * @return настроенный {@link CircuitBreaker} с именем {@code "authorization"}
     */
    public static CircuitBreaker authorizationCircuitBreaker(SwitchProperties properties) {
        SwitchProperties.CircuitBreakerProperties cb = properties.circuitBreaker().authorization();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.failureRateThreshold())
                .slidingWindowSize(cb.slidingWindowSize())
                .minimumNumberOfCalls(cb.minimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofMillis(cb.waitDurationInOpenStateMs()))
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("authorization", config);
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> LOG.warn(
                        "authorization circuit breaker: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));

        return circuitBreaker;
    }
}
