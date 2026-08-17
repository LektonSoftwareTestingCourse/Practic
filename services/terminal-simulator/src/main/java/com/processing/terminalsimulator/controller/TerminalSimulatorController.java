package com.processing.terminalsimulator.controller;

import com.processing.common.dto.terminalsimulator.TerminalRunRequest;
import com.processing.common.dto.terminalsimulator.TerminalRunResponse;
import com.processing.common.dto.terminalsimulator.TerminalStartContinuousRequest;
import com.processing.terminalsimulator.service.TerminalSimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulator/terminal")
@RequiredArgsConstructor
public class TerminalSimulatorController {

    private final TerminalSimulatorService simulatorService;

    @PostMapping("/run")
    public ResponseEntity<TerminalRunResponse> run(@Valid @RequestBody TerminalRunRequest request) {
        TerminalRunResponse response = simulatorService.run(request.count(), request.scenario(), request.tps());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start-continuous")
    @Operation(summary = "Старт непрерывного режима",
            description = "Запускает бесконечную генерацию транзакций с фиксированным TPS")
    public ResponseEntity<Void> startContinuous(
            @Valid @RequestBody TerminalStartContinuousRequest request) {
        simulatorService.startContinuous(request.tps(), request.transactionType());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stop")
    @Operation(summary = "Остановка симуляции",
            description = "Мягко останавливает бесконечный режим, дожидаясь завершения летящих транзакций")
    public ResponseEntity<Void> stopContinuous() {
        simulatorService.stopContinuous();
        return ResponseEntity.ok().build();
    }
}
