package com.processing.gateway.config;

import lombok.RequiredArgsConstructor;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Declares gateway infrastructure beans shared by filters and services.
 */
@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {

    /**
     * Creates the cache manager used by response caching.
     *
     * @return caffeine-backed cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        var cacheManager = new CaffeineCacheManager("gateway-cache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());

        return cacheManager;
    }

    /**
     * Exposes the gateway cache as a direct bean for filters.
     *
     * @param cacheManager configured cache manager
     * @return cache named {@code gateway-cache}
     */
    @Bean
    public Cache gatewayCache(CacheManager cacheManager) {
        return cacheManager.getCache("gateway-cache");
    }
}
