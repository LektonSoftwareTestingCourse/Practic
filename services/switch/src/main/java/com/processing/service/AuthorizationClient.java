package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.config.SwitchProperties;
import com.processing.exception.AuthorizationException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * HTTP-клиент для взаимодействия с Authorization Service (authorize, rollback, health).
 */
@Service
public class AuthorizationClient {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationClient.class);

    private final SwitchProperties switchProperties;
    private final RestClient restClient;
    private final Retry authorizationRetry;
    private final CircuitBreaker authorizationCircuitBreaker;

    /**
     * @param switchProperties            конфигурация URL и retry
     * @param restClient                  REST-клиент с таймаутами для Authorization
     * @param authorizationRetry          политика повторных попыток
     * @param authorizationCircuitBreaker circuit breaker для authorize
     */
    public AuthorizationClient(
            SwitchProperties switchProperties,
            RestClient restClient,
            @Qualifier("authorizationRetry") Retry authorizationRetry,
            @Qualifier("authorizationCircuitBreaker") CircuitBreaker authorizationCircuitBreaker) {
        this.switchProperties = switchProperties;
        this.restClient = restClient;
        this.authorizationRetry = authorizationRetry;
        this.authorizationCircuitBreaker = authorizationCircuitBreaker;
    }

    /**
     * Отправляет запрос авторизации с circuit breaker и retry при сетевых сбоях.
     *
     * @param request запрос с заполненным {@code issuerId}
     * @return ответ Authorization (APPROVED или DECLINED)
     * @throws AuthorizationException если сервис недоступен, контур OPEN или исчерпаны попытки
     */
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        int maxAttempts = switchProperties.retry().maxAttempts();
        try {
            return CircuitBreaker.decorateCallable(authorizationCircuitBreaker,
                    () -> Retry.decorateCallable(authorizationRetry, () -> callAuthorize(request)).call()
            ).call();
        } catch (CallNotPermittedException e) {
            throw new AuthorizationException(request.stan(), maxAttempts, "Circuit breaker open");
        } catch (Exception e) {
            String lastError = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new AuthorizationException(request.stan(), maxAttempts, lastError);
        }
    }

    /**
     * Выполняет один HTTP-вызов {@code POST /api/internal/authorize}.
     *
     * @param request тело запроса
     * @return валидный ответ Authorization
     * @throws IllegalStateException если тело ответа пустое или статус не APPROVED/DECLINED
     */
    private AuthorizationResponse callAuthorize(AuthorizationRequest request) {
        AuthorizationResponse response = restClient.post()
                .uri(switchProperties.authorizationUrl() + "/api/internal/authorize")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> { })
                .body(AuthorizationResponse.class);
        if (response != null
                && (AuthorizationResponse.STATUS_APPROVED.equals(response.status())
                || AuthorizationResponse.STATUS_DECLINED.equals(response.status()))) {
            return response;
        }
        throw new IllegalStateException("Invalid response from Authorization");
    }

    /**
     * Откатывает резервирование средств через {@code POST /api/internal/rollback}.
     * Ошибки перехватываются и логируются — вызывающий код получает {@code null}.
     *
     * @param original исходный запрос авторизации (PAN, amount)
     * @param rrn      RRN одобренной транзакции
     * @return ответ rollback или {@code null} при сбое
     */
    public RollbackResponse rollback(AuthorizationRequest original, String rrn) {
        RollbackRequest request = new RollbackRequest(rrn, original.pan(), original.amount());
        try {
            RollbackResponse response = restClient.post()
                    .uri(switchProperties.authorizationUrl() + "/api/internal/rollback")
                    .body(request)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> { })
                    .body(RollbackResponse.class);
            if (response != null
                    && (RollbackResponse.STATUS_APPROVED.equals(response.status())
                    || RollbackResponse.STATUS_DECLINED.equals(response.status()))) {
                return response;
            }
            throw new IllegalStateException("Invalid rollback response from Authorization");
        } catch (Exception e) {
            LOG.error("Rollback failed for STAN={} rrn={}", original.stan(), rrn, e);
            return null;
        }
    }

    /**
     * Проверяет доступность Authorization Service через {@code GET /health}.
     *
     * @return {@code "ok"} если сервис отвечает, иначе {@code "down"}
     */
    public String checkHealth() {
        try {
            restClient.get()
                    .uri(switchProperties.authorizationUrl() + "/health")
                    .retrieve()
                    .toBodilessEntity();
            return "ok";
        } catch (Exception e) {
            LOG.warn("Authorization health check failed", e);
            return "down";
        }
    }
}
