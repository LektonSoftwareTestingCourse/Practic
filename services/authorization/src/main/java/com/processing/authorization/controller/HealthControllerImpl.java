package com.processing.authorization.controller;

import com.processing.authorization.dto.HealthResponse;
import com.processing.authorization.services.HealthServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Endpoint for checking service health and dependencies")
public class HealthControllerImpl implements HealthController {
    private final HealthServiceImpl healthService;

    @GetMapping("/health")
    @Operation(
        summary = "Health check endpoint",
        description = "Returns the health status of the authorization service and all its dependencies"
        )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "All services are healthy",
                content = @Content(schema = @Schema(implementation = HealthResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "Service is degraded (one or more dependencies are unhealthy)",
                content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    })
    public ResponseEntity<HealthResponse> health() {
        Map<String, String> checks = healthService.healthCheckAllServices();
        boolean isAllHealthy = checks.values().stream()
                .allMatch(status -> "ok".equalsIgnoreCase(status));
        HttpStatus httpStatus = isAllHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(new HealthResponse(
                isAllHealthy ? "ok" : "degraded",
                "authorization",
                checks));
    }
}
