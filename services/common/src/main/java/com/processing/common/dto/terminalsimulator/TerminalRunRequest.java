package com.processing.common.dto.terminalsimulator;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TerminalRunRequest(
    @NotNull
    @Min(1)
    int count,
    @NotNull
    TerminalScenario scenario,
    @NotNull
    @Min(1)
    int tps
) {}
