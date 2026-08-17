package com.processing.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Thread-safe token bucket rate limiter backed by a Caffeine cache.
 */
@Component
public class InMemoryRateLimiter {

    private final int capacity;
    private final double refillTokensPerSecond;
    private final Ticker ticker;
    private final Cache<String, TokenBucket> buckets;

    /**
     * Creates a token bucket rate limiter from gateway configuration.
     *
     * @param capacity maximum number of tokens stored in each bucket
     * @param refillTokensPerSecond number of tokens refilled per second
     * @param bucketExpireAfterAccess bucket time-to-live after last access
     * @param maxBuckets maximum number of buckets kept in memory
     */
    @Autowired
    public InMemoryRateLimiter(
            @Value("${gateway.rate-limit.transactions.capacity:100}")
            int capacity,
            @Value("${gateway.rate-limit.transactions.refill-per-second:100}")
            double refillTokensPerSecond,
            @Value("${gateway.rate-limit.transactions.bucket-expire-after-access:10m}")
            Duration bucketExpireAfterAccess,
            @Value("${gateway.rate-limit.transactions.max-buckets:10000}")
            long maxBuckets
    ) {
        this(capacity, refillTokensPerSecond, bucketExpireAfterAccess, maxBuckets, Ticker.systemTicker());
    }

    /**
     * Creates a rate limiter with a custom ticker for unit tests.
     *
     * @param capacity maximum number of tokens stored in each bucket
     * @param refillTokensPerSecond number of tokens refilled per second
     * @param bucketExpireAfterAccess bucket time-to-live after last access
     * @param maxBuckets maximum number of buckets kept in memory
     * @param ticker ticker used by the token bucket and Caffeine cache
     * @return configured rate limiter
     */
    public static InMemoryRateLimiter forTesting(int capacity,
                                                 double refillTokensPerSecond,
                                                 Duration bucketExpireAfterAccess,
                                                 long maxBuckets,
                                                 Ticker ticker) {
        return new InMemoryRateLimiter(capacity, refillTokensPerSecond, bucketExpireAfterAccess, maxBuckets, ticker);
    }

    InMemoryRateLimiter(int capacity,
                        double refillTokensPerSecond,
                        Duration bucketExpireAfterAccess,
                        long maxBuckets,
                        Ticker ticker) {
        this.capacity = Math.max(0, capacity);
        this.refillTokensPerSecond = Math.max(0, refillTokensPerSecond);
        this.ticker = ticker;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(bucketExpireAfterAccess)
                .maximumSize(maxBuckets)
                .ticker(ticker)
                .build();
    }

    /**
     * Attempts to consume one token from a rate-limit bucket.
     *
     * @param key logical bucket key, for example {@code POST /api/transactions:127.0.0.1}
     * @return {@code true} when the request is within the limit
     */
    public boolean allowRequest(String key) {
        TokenBucket bucket = buckets.get(key, ignored -> new TokenBucket(capacity, refillTokensPerSecond, ticker.read()));
        assert bucket != null;
        return bucket.tryConsume(ticker.read());
    }

    /**
     * Triggers Caffeine maintenance for expired bucket cleanup.
     */
    public void cleanUp() {
        buckets.cleanUp();
    }

    private static final class TokenBucket {
        private final int capacity;
        private final double refillTokensPerSecond;
        private double tokens;
        private long lastRefillNanos;

        private TokenBucket(int capacity, double refillTokensPerSecond, long createdAtNanos) {
            this.capacity = capacity;
            this.refillTokensPerSecond = refillTokensPerSecond;
            this.tokens = capacity;
            this.lastRefillNanos = createdAtNanos;
        }

        private synchronized boolean tryConsume(long nowNanos) {
            refill(nowNanos);

            if (tokens < 1) {
                return false;
            }

            tokens -= 1;
            return true;
        }

        private void refill(long nowNanos) {
            if (nowNanos <= lastRefillNanos) {
                return;
            }

            double elapsedSeconds = (nowNanos - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillTokensPerSecond);
            lastRefillNanos = nowNanos;
        }
    }
}
