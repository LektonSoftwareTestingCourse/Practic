package com.processing.gateway.circuitbreaker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory circuit breaker for downstream services.
 *
 * <p>The breaker keeps an independent state per service name. After the
 * configured number of failures it opens the circuit, rejects requests for the
 * configured duration, and then allows one half-open probe request.</p>
 */
@Component
public class InMemoryCircuitBreaker {
    private final Duration openDuration;
    private final int failureThreshold;
    private final Clock clock;
    private final ConcurrentMap<String, CircuitState> states = new ConcurrentHashMap<>();

    /**
     * Creates a circuit breaker from application properties.
     *
     * @param openDuration time while an opened circuit rejects requests
     * @param failureThreshold number of failures needed to open the circuit
     */
    @Autowired
    public InMemoryCircuitBreaker(
            @Value("${gateway.circuit-breaker.open-duration:10s}") Duration openDuration,
            @Value("${gateway.circuit-breaker.failure-threshold:3}") int failureThreshold
    ) {
        this(openDuration, failureThreshold, Clock.systemUTC());
    }

    /**
     * Creates a circuit breaker with a custom clock for deterministic tests.
     *
     * @param openDuration time while an opened circuit rejects requests
     * @param failureThreshold number of failures needed to open the circuit
     * @param clock clock used to measure the open interval
     * @return configured circuit breaker instance
     */
    public static InMemoryCircuitBreaker forTesting(Duration openDuration, int failureThreshold, Clock clock) {
        return new InMemoryCircuitBreaker(openDuration, failureThreshold, clock);
    }

    InMemoryCircuitBreaker(Duration openDuration, int failureThreshold, Clock clock) {
        this.openDuration = openDuration;
        this.failureThreshold = failureThreshold;
        this.clock = clock;
    }

    /**
     * Returns whether a request may be sent to the given downstream service.
     *
     * @param serviceName downstream service identifier
     * @return {@code true} when the request can proceed
     */
    public boolean allowRequest(String serviceName) {
        CircuitState state = states.computeIfAbsent(serviceName, ignored -> new CircuitState());
        Instant now = clock.instant();

        synchronized (state) {
            if (state.status == Status.CLOSED) {
                return true;
            }

            if (state.status == Status.OPEN && now.isBefore(state.openedAt.plus(openDuration))) {
                return false;
            }

            state.status = Status.HALF_OPEN;
            if (state.halfOpenRequestInProgress) {
                return false;
            }

            state.halfOpenRequestInProgress = true;
            return true;
        }
    }

    /**
     * Records a successful downstream call and closes the circuit.
     *
     * @param serviceName downstream service identifier
     */
    public void recordSuccess(String serviceName) {
        CircuitState state = states.computeIfAbsent(serviceName, ignored -> new CircuitState());

        synchronized (state) {
            state.status = Status.CLOSED;
            state.failureCount = 0;
            state.halfOpenRequestInProgress = false;
            state.openedAt = Instant.EPOCH;
        }
    }

    /**
     * Records a failed downstream call and opens the circuit when the threshold
     * is reached.
     *
     * @param serviceName downstream service identifier
     */
    public void recordFailure(String serviceName) {
        CircuitState state = states.computeIfAbsent(serviceName, ignored -> new CircuitState());
        Instant now = clock.instant();

        synchronized (state) {
            state.halfOpenRequestInProgress = false;

            if (state.status == Status.HALF_OPEN) {
                open(state, now);
                return;
            }

            state.failureCount++;
            if (state.failureCount >= failureThreshold) {
                open(state, now);
            }
        }
    }

    private void open(CircuitState state, Instant now) {
        state.status = Status.OPEN;
        state.failureCount = failureThreshold;
        state.openedAt = now;
    }

    private enum Status {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private static final class CircuitState {
        private Status status = Status.CLOSED;
        private int failureCount;
        private Instant openedAt = Instant.EPOCH;
        private boolean halfOpenRequestInProgress;
    }

    /**
     * Releases the half-open probe slot without changing failure counters.
     *
     * @param serviceName downstream service identifier
     */
    public void releaseRequest(String serviceName) {
        CircuitState state = states.computeIfAbsent(serviceName, ignored -> new CircuitState());

        synchronized (state) {
            state.halfOpenRequestInProgress = false;
        }
    }
}
