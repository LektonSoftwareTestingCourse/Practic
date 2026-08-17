package com.processing.gateway.health;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for downstream health-check HTTP requests.
 */
@ConfigurationProperties(prefix = "gateway.health")
@Component
@Data
public class HealthProperties {
    private Integer connectionTimeout = 10;
    private Integer requestTimeout = 3;
    private String url = "/health";
}
