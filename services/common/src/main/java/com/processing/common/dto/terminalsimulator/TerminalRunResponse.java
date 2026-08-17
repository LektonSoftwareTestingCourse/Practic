package com.processing.common.dto.terminalsimulator;

import com.processing.common.dto.authorization.AuthorizationResponse;

import java.util.List;

public record TerminalRunResponse(
    int totalSubmitted,
    int approved,
    int declined,
    long elapsedMs,
    List<AuthorizationResponse> transactions
) {}
