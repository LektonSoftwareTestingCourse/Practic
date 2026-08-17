package com.processing.merchantacquirer.service.dto;

import com.processing.common.dto.authorization.AuthorizationResponse;
import java.util.List;

public record SimulatorStats(List<AuthorizationResponse> responses, int approved, int declined) {}
