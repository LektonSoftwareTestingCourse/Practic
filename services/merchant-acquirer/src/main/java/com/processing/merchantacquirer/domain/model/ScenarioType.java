package com.processing.merchantacquirer.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Тип сценария симуляции")
public enum ScenarioType {
  grocery,
  electronics,
  restaurant,
  travel
}
