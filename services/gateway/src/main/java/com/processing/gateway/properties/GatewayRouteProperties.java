package com.processing.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds Spring Cloud Gateway route definitions used by custom filters.
 *
 * <p>The resolver reads route predicates and {@code serviceName} metadata from
 * this structure so filters do not duplicate route-to-service mappings.</p>
 */
@Component
@ConfigurationProperties(prefix = "spring.cloud.gateway")
@Data
public class GatewayRouteProperties {

    private List<RouteDefinition> routes = new ArrayList<>();

    /**
     * Minimal route definition model needed by gateway filters.
     */
    @Data
    public static class RouteDefinition {
        private String id;
        private String uri;
        private Map<String, String> metadata = new HashMap<>();
        private List<String> predicates = new ArrayList<>();
        private List<String> filters = new ArrayList<>();
    }
}
