package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.model.Scenario;
import com.processing.merchantacquirer.domain.model.ScenarioProperties;
import com.processing.merchantacquirer.domain.model.ScenarioType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScenarioProvider {
  private final ScenarioProperties scenarioProperties;

  public Scenario getScenario(ScenarioType type) {
    return scenarioProperties.scenarios().get(type);
  }
}
