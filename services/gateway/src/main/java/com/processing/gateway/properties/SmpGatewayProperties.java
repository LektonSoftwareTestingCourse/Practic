package com.processing.gateway.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Top-level gateway configuration properties.
 */
@ConfigurationProperties(prefix = "gateway")
@Component
@Data
public class SmpGatewayProperties {
    private String version;
    private LoggingProperties logging;

    @Data
    @AllArgsConstructor
    public static class LoggingProperties {
        private Boolean bodies;
        private Boolean pretty;
        private Set<String> excludedRoutes;
    }
}
