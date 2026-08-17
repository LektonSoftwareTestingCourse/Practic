package com.processing.transactionlogger.controller;

import com.processing.transactionlogger.dto.HealthResponse;
import com.processing.transactionlogger.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер проверки работоспособности сервиса.
 * Используется Gateway для мониторинга состояния transaction-logger.
 */
@Tag(name = "Health", description = "Состояние сервиса")
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final TransactionRepository transactionRepository;

    /**
     * Возвращает статус сервиса и общее количество хранящихся транзакций
     *
     * @return HTTP 200 с {@link HealthResponse}
     */
    @Operation(summary = "Health-check")
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                "ok",
                "transaction-logger",
                transactionRepository.count()
        ));
    }
}
