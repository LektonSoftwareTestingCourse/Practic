package com.processing.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Publishes gateway-specific Micrometer counters used by the Grafana dashboard.
 */
@Component
@RequiredArgsConstructor
public class GatewayMetrics {

    private static final String REJECTED_REQUESTS = "gateway.requests.rejected";
    private static final String DOWNSTREAM_UNAVAILABLE = "gateway.downstream.unavailable";
    private static final String CACHE_REQUESTS = "gateway.cache.requests";
    private static final String CACHE_INVALIDATIONS = "gateway.cache.invalidations";
    private static final String CARDS_CACHE = "cards";
    private static final String GATEWAY_SERVICE = "gateway";

    private final MeterRegistry meterRegistry;

    /**
     * Registers expected series at startup so local dashboards show zeros before
     * the first gateway event happens.
     */
    @PostConstruct
    void registerMeters() {
        rejectedRequests("rate_limit", GATEWAY_SERVICE);
        rejectedRequests("validation_invalid_json", GATEWAY_SERVICE);
        rejectedRequests("validation_invalid_request", GATEWAY_SERVICE);
        rejectedRequests("shutting_down", GATEWAY_SERVICE);

        registerDownstreamUnavailable("switch");
        registerDownstreamUnavailable("authorization");
        registerDownstreamUnavailable("cardManagement");
        registerDownstreamUnavailable("logger");
        registerDownstreamUnavailable("terminalSimulator");
        registerDownstreamUnavailable("merchantSimulator");

        registerCardsCacheRequest("hit");
        registerCardsCacheRequest("miss");
        registerCardsCacheInvalidation();
    }

    public void recordRateLimitRejected() {
        rejectedRequests("rate_limit", GATEWAY_SERVICE).increment();
    }

    public void recordValidationRejected(String reason) {
        rejectedRequests("validation_" + reason, GATEWAY_SERVICE).increment();
    }

    public void recordCircuitOpen(String serviceName) {
        rejectedRequests("circuit_open", serviceName).increment();
    }

    public void recordGracefulShutdownRejected() {
        rejectedRequests("shutting_down", GATEWAY_SERVICE).increment();
    }

    public void recordDownstreamUnavailable(String serviceName) {
        registerDownstreamUnavailable(serviceName).increment();
    }

    public void recordCardsCacheHit() {
        recordCardsCacheRequest("hit");
    }

    public void recordCardsCacheMiss() {
        recordCardsCacheRequest("miss");
    }

    public void recordCardsCacheInvalidation() {
        registerCardsCacheInvalidation().increment();
    }

    private Counter rejectedRequests(String reason, String serviceName) {
        return Counter.builder(REJECTED_REQUESTS)
                .tag("reason", reason)
                .tag("service", serviceName)
                .register(meterRegistry);
    }

    private void recordCardsCacheRequest(String result) {
        registerCardsCacheRequest(result).increment();
    }

    private Counter registerDownstreamUnavailable(String serviceName) {
        return Counter.builder(DOWNSTREAM_UNAVAILABLE)
                .tag("service", serviceName)
                .register(meterRegistry);
    }

    private Counter registerCardsCacheRequest(String result) {
        return Counter.builder(CACHE_REQUESTS)
                .tag("cache", CARDS_CACHE)
                .tag("result", result)
                .register(meterRegistry);
    }

    private Counter registerCardsCacheInvalidation() {
        return Counter.builder(CACHE_INVALIDATIONS)
                .tag("cache", CARDS_CACHE)
                .register(meterRegistry);
    }
}
