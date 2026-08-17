package com.processing.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.processing.config.SwitchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * HTTP-клиент Merchant Acquirer Simulator для получения комиссии эквайринга.
 */
@Service
public class MerchantAcquirerClient implements AcquiringFeeClient {

    private static final Logger LOG = LoggerFactory.getLogger(MerchantAcquirerClient.class);

    private final SwitchProperties switchProperties;
    private final RestClient restClient;

    /**
     * @param switchProperties конфигурация с URL Merchant Acquirer
     * @param restClient       общий REST-клиент Switch
     */
    public MerchantAcquirerClient(SwitchProperties switchProperties, RestClient restClient) {
        this.switchProperties = switchProperties;
        this.restClient = restClient;
    }

    /**
     * {@inheritDoc}
     * При недоступности сервиса возвращает {@code null} и пишет предупреждение в лог.
     */
    @Override
    public BigDecimal fetchAcquiringFee(
            Instant transmissionDateTime,
            String stan,
            String pan,
            String terminalId,
            BigDecimal amount
    ) {
        try {
            AcquirerFeeResponse response = restClient.post()
                    .uri(switchProperties.merchantAcquirerUrl()
                            + "/api/simulator/merchant/fee")
                    .body(new AcquirerFeeRequest(
                            transmissionDateTime, stan, pan, terminalId, amount))
                    .retrieve()
                    .body(AcquirerFeeResponse.class);
            if (response == null) {
                return null;
            }
            return response.acquirerFee();
        } catch (Exception e) {
            LOG.warn("Acquiring fee unavailable for STAN={}: {}", stan, e.getMessage());
            return null;
        }
    }

    /** Тело запроса комиссии к Merchant Acquirer. */
    private record AcquirerFeeRequest(
            Instant transmissionDateTime,
            String stan,
            String pan,
            @JsonProperty("terminalId") String terminalId,
            BigDecimal amount
    ) {
    }

    /** Ответ Merchant Acquirer с рассчитанной комиссией. */
    private record AcquirerFeeResponse(
            BigDecimal acquirerFee
    ) {
    }
}
