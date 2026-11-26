package com.processing.gateway.circuitbreaker;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCircuitBreakerTest {
    @Test
    void shouldOpenAfterFailureThresholdAndAllowHalfOpenProbeAfterDelay() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-12T10:00:00Z"));
        InMemoryCircuitBreaker breaker = InMemoryCircuitBreaker.forTesting(Duration.ofSeconds(10), 2, clock);

        assertTrue(breaker.allowRequest("authorization"));
        breaker.recordFailure("authorization");
        breaker.recordFailure("authorization");
        assertFalse(breaker.allowRequest("authorization"));

        clock.advance(Duration.ofSeconds(10));
        assertTrue(breaker.allowRequest("authorization"));
        assertFalse(breaker.allowRequest("authorization"));
        breaker.recordSuccess("authorization");
        assertTrue(breaker.allowRequest("authorization"));
    }

    @Test
    void shouldReopenWhenHalfOpenProbeFails() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-12T10:00:00Z"));
        InMemoryCircuitBreaker breaker = InMemoryCircuitBreaker.forTesting(Duration.ofSeconds(5), 1, clock);
        breaker.recordFailure("card-management");
        assertFalse(breaker.allowRequest("card-management"));

        clock.advance(Duration.ofSeconds(5));
        assertTrue(breaker.allowRequest("card-management"));
        breaker.recordFailure("card-management");
        assertFalse(breaker.allowRequest("card-management"));
    }

    @Test
    void shouldKeepCircuitStateIndependentPerService() {
        MutableClock clock = new MutableClock(Instant.EPOCH);
        InMemoryCircuitBreaker breaker = InMemoryCircuitBreaker.forTesting(Duration.ofSeconds(10), 1, clock);
        breaker.recordFailure("authorization");

        assertFalse(breaker.allowRequest("authorization"));
        assertTrue(breaker.allowRequest("card-management"));
    }

    private static final class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
