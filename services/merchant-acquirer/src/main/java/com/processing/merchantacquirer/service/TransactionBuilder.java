package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.domain.service.FeeCalculator;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.model.Scenario;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.factory.AuthorizationRequestFactory;
import com.processing.common.dto.authorization.AuthorizationRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import com.processing.merchantacquirer.service.dto.RequestFeeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionBuilder {
    private final AuthorizationRequestFactory authorizationRequestFactory;
    private final TerminalProvider terminalProvider;
    private final FeeCalculator feeCalculator;

    public List<RequestFeeData> build(
            int count,
            List<CardDataResponse> cardDataResponses,
            List<Merchant> merchants,
            Scenario scenario) {

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<RequestFeeData>> futures = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                Future<RequestFeeData> future = executor.submit(
                        () -> {
                            CardDataResponse card = cardDataResponses.get(
                                    ThreadLocalRandom.current().nextInt(cardDataResponses.size()));
                            Merchant merchant = merchants.get(ThreadLocalRandom.current().nextInt(merchants.size()));
                            Terminal terminal = terminalProvider.getByMerchant(merchant.getId());
                            BigDecimal amount = new BigDecimal(
                                    Math.clamp(
                                            ThreadLocalRandom.current().nextDouble(
                                                    merchant.getAverageCheck().doubleValue() * 0.5,
                                                    merchant.getAverageCheck().doubleValue() * 2),
                                            scenario.getCountLower().doubleValue(),
                                            scenario.getCountUpper().doubleValue()
                                    )).setScale(0, RoundingMode.HALF_EVEN);

                            LocalDate date = LocalDate.now();
                            Instant beforeTime = date.atTime(
                                    LocalTime.parse(scenario.getTimeLower())).toInstant(ZoneOffset.ofHours(3));
                            Instant afterTime = date.atTime(
                                    LocalTime.parse(scenario.getTimeUpper())).toInstant(ZoneOffset.ofHours(3));
                            var duration = Duration.between(beforeTime, afterTime).toMillis();
                            Instant transactionTime = beforeTime.plusMillis(ThreadLocalRandom.current().nextLong(0, duration));

                            AuthorizationRequest authorizationRequest = authorizationRequestFactory.build(
                                    card.pan(), card.currencyCode(), amount, terminal, merchant, transactionTime);

                            BigDecimal fee = feeCalculator.calculate(
                                    merchant.getAcquiringFee(),
                                    authorizationRequest.amount());
                            AcquirerFee acquirerFee = AcquirerFee.of(fee, authorizationRequest);

                            return new RequestFeeData(authorizationRequest, acquirerFee);
                        }
                );
                futures.add(future);
            }

            List<RequestFeeData> requests = new ArrayList<>(count);

            for (Future<RequestFeeData> future : futures) {
                try {
                    requests.add(future.get());
                } catch (ExecutionException e) {
                    throw new IllegalStateException("Failed build authorization request", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while build auhtorization request", e);
                }
            }
            return requests;
        }
    }
}
