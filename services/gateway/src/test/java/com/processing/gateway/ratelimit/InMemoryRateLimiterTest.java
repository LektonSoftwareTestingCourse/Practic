package com.processing.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterTest {
    @Test
    void shouldAllowRequestsUpToBucketCapacity() {
        MutableTicker ticker = new MutableTicker();
        InMemoryRateLimiter limiter = InMemoryRateLimiter.forTesting(
                2, 0, Duration.ofMinutes(1), 100, ticker);

        assertTrue(limiter.allowRequest("client-a"));
        assertTrue(limiter.allowRequest("client-a"));
        assertFalse(limiter.allowRequest("client-a"));
    }

    @Test
    void shouldRefillTokensAfterElapsedTime() {
        MutableTicker ticker = new MutableTicker();
        InMemoryRateLimiter limiter = InMemoryRateLimiter.forTesting(
                1, 1, Duration.ofMinutes(1), 100, ticker);

        assertTrue(limiter.allowRequest("client-a"));
        assertFalse(limiter.allowRequest("client-a"));
        ticker.advance(Duration.ofSeconds(1));
        assertTrue(limiter.allowRequest("client-a"));
    }

    @Test
    void shouldKeepIndependentBucketsForDifferentKeys() {
        MutableTicker ticker = new MutableTicker();
        InMemoryRateLimiter limiter = InMemoryRateLimiter.forTesting(
                1, 0, Duration.ofMinutes(1), 100, ticker);

        assertTrue(limiter.allowRequest("client-a"));
        assertTrue(limiter.allowRequest("client-b"));
        assertFalse(limiter.allowRequest("client-a"));
    }

    private static final class MutableTicker implements Ticker {
        private long nanos;

        @Override
        public long read() {
            return nanos;
        }

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }
    }
}
