package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.exception.AuthorizationException;
import com.processing.exception.UnknownBinException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Оркестратор маршрутизации: BIN → Authorization → Logger с обработкой ошибок и rollback.
 */
@Service
public class RouteService {

    private static final Logger LOG = LoggerFactory.getLogger(RouteService.class);

    private final RoutingService routingService;
    private final AuthorizationClient authorizationClient;
    private final AcquiringFeeClient acquiringFeeClient;
    private final LoggerClient loggerClient;

    /**
     * @param routingService        сервис определения issuerId по BIN
     * @param authorizationClient   HTTP-клиент Authorization Service
     * @param acquiringFeeClient    клиент получения комиссии эквайринга
     * @param loggerClient          клиент публикации транзакций в RabbitMQ
     */
    public RouteService(
            RoutingService routingService,
            AuthorizationClient authorizationClient,
            AcquiringFeeClient acquiringFeeClient,
            LoggerClient loggerClient) {
        this.routingService = routingService;
        this.authorizationClient = authorizationClient;
        this.acquiringFeeClient = acquiringFeeClient;
        this.loggerClient = loggerClient;
    }

    /**
     * Выполняет полный цикл обработки транзакции: маршрутизация, авторизация, логирование.
     * При недоступности RabbitMQ после APPROVED инициирует rollback и возвращает код {@code 96}.
     *
     * @param request входящий запрос авторизации от Gateway
     * @return ответ авторизации для клиента
     */
    public AuthorizationResponse route(AuthorizationRequest request) {
        long startMs = System.currentTimeMillis();
        String pan = request.pan();
        String bin = pan != null && pan.length() >= 6 ? pan.substring(0, 6) : "??????";

        AuthorizationRequest normalizedRequest = AuthorizationRequestNormalizer.normalize(request);
        AuthorizationRequest routedRequest = normalizedRequest;
        AuthorizationResponse response;
        BigDecimal acquiringFee = null;
        String issuerId = null;

        try {
            issuerId = routingService.getIssuerIdByPan(pan);
            routedRequest = normalizedRequest.withIssuerId(issuerId);
            response = authorizationClient.authorize(routedRequest);
            acquiringFee = acquiringFeeClient.fetchAcquiringFee(
                    routedRequest.transmissionDateTime(),
                    routedRequest.stan(),
                    routedRequest.pan(),
                    routedRequest.terminalId(),
                    routedRequest.amount());
        } catch (UnknownBinException e) {
            LOG.warn("TX {} | BIN={} → unknown BIN | DECLINED", request.stan(), bin);
            response = AuthorizationResponse.unknownBin(request.stan());
        } catch (AuthorizationException e) {
            LOG.error("{}", e.getMessage());
            response = AuthorizationResponse.authUnavailable(request.stan());
        }

        TransactionRequest transaction = buildTransaction(routedRequest, response, acquiringFee);
        boolean logged = tryLog(transaction);

        if (!logged && AuthorizationResponse.STATUS_APPROVED.equals(response.status())) {
            LOG.error("Queue publish failed for TX {} — rolling back reservation", request.stan());
            RollbackResponse rollback = authorizationClient.rollback(routedRequest, response.rrn());
            logRollbackResult(request.stan(), rollback);
            return AuthorizationResponse.systemError(request.stan());
        }

        long elapsed = System.currentTimeMillis() - startMs;
        LOG.info("TX {} | BIN={} → {} | Status={} | {}ms",
                request.stan(), bin, issuerId, response.status(), elapsed);

        return response;
    }

    /**
     * Публикует транзакцию в RabbitMQ для асинхронной доставки в Logger.
     *
     * @param transaction DTO для публикации в очередь
     * @return {@code true}, если брокер принял сообщение
     */
    private boolean tryLog(TransactionRequest transaction) {
        return loggerClient.log(transaction);
    }

    /**
     * Логирует результат rollback-запроса в Authorization.
     *
     * @param stan     STAN исходной транзакции
     * @param rollback ответ Authorization на rollback; может быть {@code null}
     */
    private void logRollbackResult(String stan, RollbackResponse rollback) {
        if (rollback == null) {
            LOG.error("Rollback failed for TX {} — no response from Authorization", stan);
            return;
        }
        if (RollbackResponse.STATUS_APPROVED.equals(rollback.status())) {
            LOG.info("Rollback succeeded for TX {} rrn={}", stan, rollback.rrn());
            return;
        }
        LOG.warn("Rollback declined for TX {} rrn={} code={} reason={}",
                stan, rollback.rrn(), rollback.responseCode(), rollback.declineReason());
    }

    /**
     * Собирает {@link TransactionRequest} из запроса, ответа Authorization и комиссии.
     *
     * @param request      нормализованный запрос с issuerId
     * @param response     ответ Authorization
     * @param acquiringFee комиссия эквайринга или {@code null}
     * @return DTO для Logger
     */
    private TransactionRequest buildTransaction(
            AuthorizationRequest request,
            AuthorizationResponse response,
            BigDecimal acquiringFee) {
        return new TransactionRequest(
                UUID.randomUUID(),
                request.mti(),
                request.stan(),
                response.rrn(),
                request.pan(),
                request.processingCode(),
                request.amount(),
                request.currencyCode(),
                request.terminalId(),
                request.terminalType(),
                request.merchantId(),
                request.mcc(),
                request.acquirerId(),
                request.issuerId(),
                acquiringFee,
                toTransactionStatus(response.status()),
                response.declineReason(),
                response.authCode(),
                response.processingTimeMs() != null ? response.processingTimeMs() : 0,
                request.transmissionDateTime(),
                Instant.now()
        );
    }

    /**
     * Преобразует строковый статус ответа в {@link TransactionStatus}.
     *
     * @param status значение поля {@code status} из {@link AuthorizationResponse}
     * @return соответствующий enum
     */
    private static TransactionStatus toTransactionStatus(String status) {
        return TransactionStatus.valueOf(status);
    }
}
