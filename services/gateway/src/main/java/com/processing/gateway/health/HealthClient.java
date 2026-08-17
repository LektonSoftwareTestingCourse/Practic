package com.processing.gateway.health;

import com.processing.gateway.health.models.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * HTTP client wrapper used by gateway health checks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HealthClient {
    private final WebClient.Builder webClientBuilder;
    private final HealthProperties healthProperties;

    /**
     * Sends a health-check request to a downstream service.
     *
     * @param url full health-check URL
     * @param serviceName service name used in logs
     * @return {@link HealthStatus#OK} for HTTP 200, otherwise
     *         {@link HealthStatus#UNAVAILABLE}
     */
    public Mono<HealthStatus> sendHealthCheckRequest(String url, String serviceName) {
        return webClientBuilder.build()
                .get()
                .uri(url)
                .exchangeToMono(response -> Mono.just(response.statusCode().value() == HttpStatus.OK.value()
                        ? HealthStatus.OK
                        : HealthStatus.UNAVAILABLE))
                .timeout(Duration.ofSeconds(healthProperties.getRequestTimeout()))
                .onErrorResume(e -> {
                    log.error("Exception occurred while checking health of service {}", serviceName, e);
                    return Mono.just(HealthStatus.UNAVAILABLE);
                });
    }
}
