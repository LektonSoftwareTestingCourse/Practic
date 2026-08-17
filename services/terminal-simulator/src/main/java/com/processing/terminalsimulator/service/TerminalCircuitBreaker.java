package com.processing.terminalsimulator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class TerminalCircuitBreaker {

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private final int errorThreshold;
    private final long cooldownMs = 3000;
    private final int successTestThreshold = 2;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private final AtomicInteger consecutiveSuccesses = new AtomicInteger(0);
    private volatile long lastStateChangeTime = 0;

    public TerminalCircuitBreaker(@Value("${simulator.errorThreshold:5}") int errorThreshold) {
        this.errorThreshold = errorThreshold;
    }

    public boolean isCallAllowed() {
        if (state.get() == State.OPEN) {
            if (System.currentTimeMillis() - lastStateChangeTime > cooldownMs) {

                consecutiveSuccesses.set(0);
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    log.info("Circuit Breaker: Entering HALF_OPEN. Testing gateway...");
                    consecutiveSuccesses.set(0);
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        consecutiveErrors.set(0);
        if (state.get() == State.HALF_OPEN) {
            if (consecutiveSuccesses.incrementAndGet() >= successTestThreshold) {
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    log.info("Circuit Breaker: CLOSED. Gateway fully recovered!");
                }
            }
        }
    }

    public void recordNetworkFailure(String reason) {
        log.warn("Network failure registered: {}", reason);
        while (true) {
            State currentState = state.get();

            if (currentState == State.OPEN) {
                return;
            }
            if (currentState == State.HALF_OPEN) {
                if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                    log.error("Circuit Breaker: HALF_OPEN failed. Back to OPEN");
                    lastStateChangeTime = System.currentTimeMillis();
                }
                return;
            } else if (currentState == State.CLOSED) {
                if (consecutiveErrors.incrementAndGet() >= errorThreshold) {
                    if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                        log.error("Circuit Breaker: OPEN. Disabling network");
                        lastStateChangeTime = System.currentTimeMillis();
                    }
                }
                return;
            }
        }
    }
}
