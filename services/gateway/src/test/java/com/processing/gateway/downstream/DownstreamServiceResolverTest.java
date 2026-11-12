package com.processing.gateway.downstream;

import com.processing.gateway.properties.GatewayRouteProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownstreamServiceResolverTest {
    @Test
    void shouldResolveServiceForMatchingPathPattern() {
        GatewayRouteProperties properties = properties(route(
                Map.of("serviceName", "authorization"), List.of("Path=/api/internal/**")));

        assertEquals("authorization", new DownstreamServiceResolver(properties)
                .resolve("/api/internal/authorize").orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenNoPatternMatches() {
        GatewayRouteProperties properties = properties(route(
                Map.of("serviceName", "authorization"), List.of("Path=/api/internal/**")));

        assertTrue(new DownstreamServiceResolver(properties).resolve("/health").isEmpty());
    }

    @Test
    void shouldIgnoreRoutesWithoutServiceNameOrPathPredicate() {
        GatewayRouteProperties.RouteDefinition noService = route(Map.of(), List.of("Path=/api/**"));
        GatewayRouteProperties.RouteDefinition noPath = route(Map.of("serviceName", "switch"), List.of("Method=POST"));

        assertTrue(new DownstreamServiceResolver(properties(noService, noPath)).resolve("/api/route").isEmpty());
    }

    private static GatewayRouteProperties properties(GatewayRouteProperties.RouteDefinition... routes) {
        GatewayRouteProperties properties = new GatewayRouteProperties();
        properties.setRoutes(List.of(routes));
        return properties;
    }

    private static GatewayRouteProperties.RouteDefinition route(Map<String, String> metadata, List<String> predicates) {
        GatewayRouteProperties.RouteDefinition route = new GatewayRouteProperties.RouteDefinition();
        route.setMetadata(metadata);
        route.setPredicates(predicates);
        return route;
    }
}
