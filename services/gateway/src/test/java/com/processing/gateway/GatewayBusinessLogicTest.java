package com.processing.gateway;

import com.github.benmanes.caffeine.cache.Ticker;
import com.processing.gateway.circuitbreaker.InMemoryCircuitBreaker;
import com.processing.gateway.logging.LogDataMasker;
import com.processing.gateway.ratelimit.InMemoryRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayBusinessLogicTest {

    @Test
    void shouldMaskPanAndCvvInLogData() {
        // Arrange
        LogDataMasker masker = new LogDataMasker();

        // Act
        String result = masker.maskData("{\"pan\":\"4000001234567890\",\"cvv\":\"123\"}");

        // Assert
        assertTrue(result.contains("400000****7890"));
        assertTrue(result.contains("***"));
        assertFalse(result.contains("123456"));
    }

    @Test
    void shouldOpenAndRecoverCircuitBreaker() {
        // Arrange
        MutableClock clock = new MutableClock(Instant.parse("2026-10-01T10:00:00Z"));
        InMemoryCircuitBreaker breaker = InMemoryCircuitBreaker.forTesting(Duration.ofSeconds(10), 2, clock);

        // Act and Assert
        assertTrue(breaker.allowRequest("authorization"));
        breaker.recordFailure("authorization");
        breaker.recordFailure("authorization");
        assertFalse(breaker.allowRequest("authorization"));
        clock.advance(Duration.ofSeconds(11));
        assertTrue(breaker.allowRequest("authorization"));
        assertFalse(breaker.allowRequest("authorization"));
        breaker.recordSuccess("authorization");
        assertTrue(breaker.allowRequest("authorization"));
    }

    @Test
    void shouldRefillRateLimitBucketOverTime() {
        // Arrange
        MutableTicker ticker = new MutableTicker();
        InMemoryRateLimiter limiter = InMemoryRateLimiter.forTesting(
                2, 1, Duration.ofMinutes(1), 100, ticker);

        // Act and Assert
        assertTrue(limiter.allowRequest("client"));
        assertTrue(limiter.allowRequest("client"));
        assertFalse(limiter.allowRequest("client"));
        ticker.advance(Duration.ofSeconds(1));
        assertTrue(limiter.allowRequest("client"));
    }

    private static final class MutableTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
