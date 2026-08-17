package com.processing.authorization.services;

import com.processing.authorization.dto.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {
    @Value("${services-to-health-check}")
    private List<String> toHealthCheck;
    private final RestClient restClient;

    public Map<String, String> healthCheckAllServices() {
        Map<String, String> result = new HashMap<>();
        log.debug("Starting to health check depended services");
        for (String serviceUrl : toHealthCheck) {
            Response healthCheckResponse = checkHealth(serviceUrl);
            result.put(healthCheckResponse.service(), healthCheckResponse.status());
        }
        return result;
    }

    private Response checkHealth(String serviceUrl) {
        try {
            log.debug("Checking health of {}", serviceUrl);
            URI uri = UriComponentsBuilder
                    .fromUriString(serviceUrl)
                    .scheme("http")
                    .path("/health")
                    .build()
                    .toUri();

            Map<String, Object> body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        log.debug("Health check failed for {}", uri);
                        throw new RuntimeException("Health check failed for " + uri);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (body == null) {
                return new Response(serviceUrl, "unknown");
            }

            Object statusObj = body.get("status");
            Object serviceObj = body.get("service");
            String status = statusObj instanceof String ? (String) statusObj : "unknown";
            String service = serviceObj instanceof String ? (String) serviceObj : serviceUrl;
            return new Response(service, status);
        } catch (Exception e) {
            log.error("Health check failed for {}", serviceUrl, e);
            return new Response(serviceUrl, "down");
        }
    }
}
