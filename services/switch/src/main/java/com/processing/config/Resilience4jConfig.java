package com.processing.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring-конфигурация бинов Resilience4j Retry и CircuitBreaker.
 */
@Configuration
public class Resilience4jConfig {

    /**
     * @param properties конфигурация Switch
     * @return retry для Authorization Service
     */
    @Bean
    public Retry authorizationRetry(SwitchProperties properties) {
        return RetryFactory.authorizationRetry(properties);
    }

    /**
     * @param properties конфигурация Switch
     * @return circuit breaker для Authorization Service
     */
    @Bean
    public CircuitBreaker authorizationCircuitBreaker(SwitchProperties properties) {
        return CircuitBreakerFactory.authorizationCircuitBreaker(properties);
    }
}
