package com.processing.terminalsimulator.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.terminalsimulator.TerminalRunResponse;
import com.processing.common.dto.terminalsimulator.TerminalScenario;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.common.dto.terminalsimulator.TransactionType;
import com.processing.terminalsimulator.factory.TransactionFactory;
import com.processing.terminalsimulator.client.GatewayClient;
import com.processing.terminalsimulator.service.ScenarioTaskGenerator.TransactionTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
@Service
public class TerminalSimulatorService {
    private final GatewayClient gatewayClient;
    private final TransactionFactory transactionFactory;
    private final ScenarioTaskGenerator taskGenerator;
    private final int cardsAmount;
    private final TerminalCircuitBreaker circuitBreaker;

    private final AtomicBoolean isContinuousRunning = new AtomicBoolean(false);
    private final AtomicReference<Thread> continuousLoopThread = new AtomicReference<>(null);  // видимость
    // из разных потоков (методы start-continuous и stop)

    public  TerminalSimulatorService(GatewayClient gatewayClient, TransactionFactory transactionFactory,
                                     ScenarioTaskGenerator taskGenerator,
                                     TerminalCircuitBreaker circuitBreaker,
                                     @Value("${simulator.cardsAmount:5000}") int cardsAmount) {
        this.gatewayClient = gatewayClient;
        this.transactionFactory = transactionFactory;
        this.taskGenerator = taskGenerator;
        this.circuitBreaker = circuitBreaker;
        this.cardsAmount = cardsAmount;
    }

    private AuthorizationResponse executeSingleTransaction(TransactionTask task, AtomicInteger approved,
                                                           AtomicInteger declined,
                                                           CardRegistry cardRegistry,
                                                           String terminalId) {
        CardModelStatus requiredStatus = transactionFactory.getRequiredStatus(task.type());
        CardModel card = cardRegistry.getRandomCard(requiredStatus);
        AuthorizationRequest tx = transactionFactory.create(task.type(), task.partOfDay(), card, terminalId);
        AuthorizationResponse authResp = gatewayClient.sendToGateway(tx);

        if (TransactionStatus.APPROVED.name().equals(authResp.status())) {
            approved.incrementAndGet();
        } else if (TransactionStatus.DECLINED.name().equals(authResp.status())) {
            declined.incrementAndGet();
            if (!authResp.declineReason().equals("INSUFFICIENT_FUNDS") && !authResp.declineReason().equals("CARD_BLOCKED")
                    && !authResp.declineReason().equals("EXCEEDS_AMOUNT_LIMIT")
                    && !authResp.declineReason().equals("CARD_NOT_FOUND")) {
                log.warn("Terminal-simulator: declined transaction in HTTP response: {}",
                        authResp.declineReason());
            }
        } else {
            log.warn("Terminal-simulator: not accepted/declined transaction in HTTP response: {}",
                    authResp.declineReason());
        }
        return authResp;
    }

    public TerminalRunResponse run(int count, TerminalScenario scenario, int tps) {
        long start = System.currentTimeMillis();
        String terminalId = String.format("TERM%04d", ThreadLocalRandom.current().nextInt(1, 10_000));
        CardRegistry cardRegistry = new CardRegistry(gatewayClient, scenario, cardsAmount);
        List<TransactionTask> tasks = taskGenerator.generateTasks(scenario, count);

        ConcurrentLinkedQueue<AuthorizationResponse> authResponses = new ConcurrentLinkedQueue<>();
        AtomicInteger approved = new AtomicInteger(0);
        AtomicInteger declined = new AtomicInteger(0);

        AtomicInteger totalSubmitted = new AtomicInteger(0);
        long delayMs = 1000 / tps;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // главный поток заходит и раздает задачи экзекутору
            for (TransactionTask task : tasks) {
                // неблокирующий метод, закидывает задачу в пул
                executor.submit(() -> {  // создаю задачу (объект Runnable), без входящих аргументов, с таким кодом:
                    totalSubmitted.incrementAndGet();

                    if (!circuitBreaker.isCallAllowed()) {
                        authResponses.add(new AuthorizationResponse(null, null, null, null,
                                null, null, "Circuit Breaker in transaction-simulator: "
                                + "Gateway is down, request skipped", 0));
                        return;
                    }
                    try {
                        AuthorizationResponse resp = executeSingleTransaction(task, approved, declined, cardRegistry,
                                terminalId);
                        authResponses.add(resp);

                        circuitBreaker.recordSuccess();
                    } catch (org.springframework.web.client.ResourceAccessException e) {
                        handleNetworkFailure(e.getMessage(), authResponses);  // лог, добавление заглушки, и либо из half_open в
                        // open, либо увеличиваем счетчик ошибок и переводим из closed в open (при достижении errorThreshold)
                    } catch (org.springframework.web.client.HttpStatusCodeException e) {
                        if (e.getStatusCode().is5xxServerError()) {
                            handleNetworkFailure("Gateway internal error(5xx): " + e.getStatusCode(), authResponses);
                        } else {
                            handleInternalFailure("Gateway client error(4xx): " + e.getMessage(), e, authResponses);
                        }
                    } catch (Exception e) {
                        handleInternalFailure("Internal terminal simulation error", e, authResponses);
                    }
                });

                Thread.sleep(delayMs);
            }
            // тут вызывается executor.close() и ждет завершения всех задач(!!) без allOf().join()
        } catch (InterruptedException e) {  // Если кто-то решит прервать главный поток, то он вызовет у главного потока
            // метод .interrupt(), который поставит внутри потока флаг interrupted = true. Это увидит Thread.sleep(),
            // выкинет InterruptedException и поставит обратно interrupted = false. Блок for прервется, успев создать
            // только часть тасок.
            log.error("Simulation loop was interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();  // возвращаем interrupted = true, чтобы для внешнего мира не выглядело
            // так, будто наш поток завершился сам, добровольно и успешно.
            // Все это делается, чтобы собрать остатки данных и вернуть хотя бы то, что успело выполниться, вместо
            // ошибки 500.
        } catch (Exception e) {
            log.error("Critical execution error in simulation main loop: {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - start;
        return new TerminalRunResponse(totalSubmitted.get(), approved.get(), declined.get(), elapsed,
                authResponses.stream().toList());
    }

    public void startContinuous(int tps, TransactionType transactionType) {
        if (!isContinuousRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Continuous simulation is already running!");
        }
        log.info("Starting continuous simulation. TPS: {}, Scenario: {}", tps, transactionType);

        long delayMs = 1000 / tps;
        String terminalId = String.format("TERM%04d", ThreadLocalRandom.current().nextInt(1, 10_000));
        TerminalScenario scenario = (transactionType == TransactionType.BLOCKED) ? TerminalScenario.declines_test
                : TerminalScenario.normal;
        CardRegistry cardRegistry = new CardRegistry(gatewayClient, scenario, cardsAmount);

        // хочу создать виртуальный поток, один-единственный фоновый поток-диспетчер
        Thread thread = Thread.ofVirtual().unstarted(() -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                // когда я в методе stopContinuous() вызываю continuousLoopThread.interrupt(), я не убиваю поток,
                // а ставлю ему внутренний флаг interrupted=true
                while (isContinuousRunning.get() && !Thread.currentThread().isInterrupted()) {
                    TransactionTask task = taskGenerator.generateSingleTask(transactionType);
                    executor.submit(() -> {
                        if (!circuitBreaker.isCallAllowed()) {
                            return;
                        }
                        try {
                            executeSingleTransaction(task, new AtomicInteger(), new AtomicInteger(), cardRegistry,
                                    terminalId);
                            circuitBreaker.recordSuccess();
                        } catch (org.springframework.web.client.ResourceAccessException
                                 | org.springframework.web.client.HttpStatusCodeException e) {
                            circuitBreaker.recordNetworkFailure(e.getMessage());
                        } catch (Exception e) {
                            log.error("Continuous transaction execution failed", e);
                        }
                    });

                    Thread.sleep(delayMs);
                }
            } catch (InterruptedException e) {
                log.info("Continuous simulation loop was interrupted: {}", e.getMessage());
            } finally {
                isContinuousRunning.set(false);
            }
        });

        continuousLoopThread.set(thread);
        thread.start();

    }

    public void stopContinuous() {
        if (isContinuousRunning.compareAndSet(true, false)) {
            Thread threadToInterrupt = continuousLoopThread.getAndSet(null);  // прерываю поток-диспетчер,
            // чтобы он вышел из Thread.sleep()
            if (threadToInterrupt != null) {
                threadToInterrupt.interrupt();
                try {
                    // жду, пока поток завершится и закроет свой экзекутор внутри try-with-resources
                    threadToInterrupt.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log.info("Continuous simulation stopped.");
            }
        }
    }

    private void handleInternalFailure(String contextMessage, Exception e,
                                       ConcurrentLinkedQueue<AuthorizationResponse> authResponses) {
        log.error("{}, but keeping simulation", contextMessage, e);
        authResponses.add(new AuthorizationResponse(null, null, null, null,
                null, null, "Internal simulation error: " + e.getMessage(), 0));
    }

    private void handleNetworkFailure(String errorMessage, ConcurrentLinkedQueue<AuthorizationResponse> authResponses) {
        log.warn("Network transaction failed: {}", errorMessage);
        circuitBreaker.recordNetworkFailure(errorMessage);

        authResponses.add(new AuthorizationResponse(null, null, null, null,
                null, null, errorMessage, 0));
    }
}
