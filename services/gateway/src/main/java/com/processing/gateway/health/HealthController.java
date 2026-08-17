package com.processing.gateway.health;

import com.processing.gateway.health.models.HealthResponse;
import com.processing.gateway.health.models.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing gateway health information.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    private final HealthService healthService;

    /**
     * Returns gateway health together with downstream service statuses.
     *
     * @return HTTP 200 when all dependencies are available, otherwise HTTP 503
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<HealthResponse>> health() {
        return healthService.getDownstreamServicesHealth()
                .map(health -> health.status() == HealthStatus.OK
                        ? ResponseEntity.ok(health)
                        : ResponseEntity.status(503).body(health));
    }
}
