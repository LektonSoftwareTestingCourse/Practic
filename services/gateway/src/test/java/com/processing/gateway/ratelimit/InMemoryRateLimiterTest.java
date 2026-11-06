package com.processing.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private static final Duration BUCKET_TTL = Duration.ofMinutes(10);
    private static final long MAX_BUCKETS = 100;

    @Test
    void shouldAllowRequestWhenCapacityIsNotExceeded() {
        InMemoryRateLimiter limiter = limiter(2, 1);

        boolean allowed = limiter.allowRequest("client-1");

        assertThat(allowed).isTrue();
    }

    @Test
    void shouldRejectRequestWhenCapacityIsExceeded() {
        InMemoryRateLimiter limiter = limiter(2, 1);
        limiter.allowRequest("client-1");
        limiter.allowRequest("client-1");

        boolean allowed = limiter.allowRequest("client-1");

        assertThat(allowed).isFalse();
    }

    @Test
    void shouldAllowRequestWhenBucketIsRefilled() {
        TestTicker ticker = new TestTicker();
        InMemoryRateLimiter limiter =
                InMemoryRateLimiter.forTesting(2, 1, BUCKET_TTL, MAX_BUCKETS, ticker);
        limiter.allowRequest("client-1");
        limiter.allowRequest("client-1");
        ticker.advance(Duration.ofSeconds(1));

        boolean allowed = limiter.allowRequest("client-1");

        assertThat(allowed).isTrue();
    }

    @Test
    void shouldKeepSeparateBucketsWhenClientsDiffer() {
        InMemoryRateLimiter limiter = limiter(1, 0);
        limiter.allowRequest("client-1");

        boolean allowed = limiter.allowRequest("client-2");

        assertThat(allowed).isTrue();
    }

    private static InMemoryRateLimiter limiter(int capacity, int refillTokens) {
        return InMemoryRateLimiter.forTesting(
                capacity,
                refillTokens,
                BUCKET_TTL,
                MAX_BUCKETS,
                new TestTicker()
        );
    }

    private static final class TestTicker implements Ticker {

        private long nanos;

        @Override
        public long read() {
            return nanos;
        }

        private void advance(Duration duration) {
            nanos += duration.toNanos();
        }
    }
}
