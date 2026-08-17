package com.processing.controller;

import com.processing.config.SwitchProperties;
import com.processing.dto.HealthResponse;
import com.processing.service.AuthorizationClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST-контроллер health-check Switch и зависимостей.
 */
@RestController
public class HealthController {

    private final SwitchProperties switchProperties;
    private final AuthorizationClient authorizationClient;

    /**
     * @param switchProperties    версия и конфигурация сервиса
     * @param authorizationClient клиент для проверки Authorization
     */
    public HealthController(SwitchProperties switchProperties, AuthorizationClient authorizationClient) {
        this.switchProperties = switchProperties;
        this.authorizationClient = authorizationClient;
    }

    /**
     * Возвращает статус Switch и доступность Authorization Service.
     *
     * @return {@link HealthResponse} с полем {@code dependencies.authorization}
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                "ok",
                "switch",
                switchProperties.version(),
                Map.of("authorization", authorizationClient.checkHealth())
        ));
    }
}
