package com.processing.merchantacquirer.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Schema(description = "Конфигурация сценариев симуляции, сопоставляющая тип сценария с его параметрами")
@ConfigurationProperties(prefix = "simulation")
public record ScenarioProperties(
    @Schema(description = "Параметры по каждому типу сценария")
    Map<ScenarioType, Scenario> scenarios) {}
