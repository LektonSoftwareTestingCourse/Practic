package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.controller.dto.*;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.model.Scenario;
import com.processing.merchantacquirer.service.dto.RequestFeeData;
import com.processing.merchantacquirer.service.dto.SimulatorStats;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SimulationService {
  private final CardProvider cardProvider;
  private final MerchantProvider merchantProvider;
  private final TransactionBuilder transactionBuilder;
  private final TransactionSender transactionSender;
  private final ScenarioProvider scenarioProvider;
  private final AcquirerProvider acquirerProvider;

  public SimulatorResponse run(SimulatorRequest request) {
    LocalDateTime startTime = LocalDateTime.now();
    log.info("Simulator request: {}", request);

    List<CardDataResponse> cards = cardProvider.getCards(request.count());
    log.info("{} cards load from card management", cards.size());

    // Получение сценария
    Scenario scenario = scenarioProvider.getScenario(request.scenario());
    log.info("Loaded scenario: {}", scenario);

    // Получение мерчантов
    List<Merchant> merchants = merchantProvider.getMerchant(request.mccCodes(), scenario);
    log.info("Merchants with MCC({}): {}", request.mccCodes(), merchants);

    // Создание транзакций
    List<RequestFeeData> built = transactionBuilder.build(request.count(), cards, merchants, scenario);
    merchants = null;
    cards = null;
    scenario = null;

    acquirerProvider.saveAll(built.stream().map(RequestFeeData::fee).toList());

    SimulatorStats stats = transactionSender.sendAll(
        built.stream().map(RequestFeeData::authorizationRequest).toList(), request.tps());

    // Формирование
    LocalDateTime endTime = LocalDateTime.now();

    return new SimulatorResponse(
        request.count(),
        stats.approved(),
        stats.declined(),
        (int) Duration.between(startTime, endTime).toMillis(),
        stats.responses());
  }

  public List<Merchant> getAllMerchants() {
    return merchantProvider.getAll();
  }

  public long countMerchants() {
    return merchantProvider.count();
  }

  public AcquirerFeeResponse getAcquirerFee(AcquirerFeeRequest request) {
    return acquirerProvider.getAcquirerFee(request);
  }
}
