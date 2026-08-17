package com.processing.gateway.health;

import com.processing.gateway.health.models.HealthResponse;
import com.processing.gateway.health.models.HealthStatus;
import com.processing.gateway.properties.SmpGatewayProperties;
import com.processing.gateway.properties.ServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Aggregates gateway and downstream health information.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {
    private static final String GATEWAY_SERVICE_NAME = "gateway";
    private static final String SWITCH_SERVICE_NAME = "switch";
    private static final String AUTH_SERVICE_NAME = "authorization";
    private static final String CARD_MGMT_SERVICE_NAME = "cardManagement";
    private static final String LOGGER_SERVICE_NAME = "logger";

    private final HealthClient healthClient;
    private final ServiceProperties serviceProperties;
    private final HealthProperties healthProperties;
    private final SmpGatewayProperties smpGatewayProperties;

    /**
     * Checks configured downstream services and builds the gateway health response.
     *
     * @return aggregated health response
     */
    public Mono<HealthResponse> getDownstreamServicesHealth() {
        String switchUrl = serviceProperties.getSwitchUrl() + healthProperties.getUrl();
        String loggerUrl = serviceProperties.getLoggerUrl() + healthProperties.getUrl();
        String authUrl = serviceProperties.getAuthUrl() + healthProperties.getUrl();
        String cardsUrl = serviceProperties.getCardsUrl() + healthProperties.getUrl();

        Mono<HealthStatus> switchHealth = healthClient.sendHealthCheckRequest(switchUrl, SWITCH_SERVICE_NAME);
        Mono<HealthStatus> authHealth = healthClient.sendHealthCheckRequest(authUrl, AUTH_SERVICE_NAME);
        Mono<HealthStatus> cardHealth = healthClient.sendHealthCheckRequest(cardsUrl, CARD_MGMT_SERVICE_NAME);
        Mono<HealthStatus> loggerHealth = healthClient.sendHealthCheckRequest(loggerUrl, LOGGER_SERVICE_NAME);

        return Mono.zip(switchHealth, authHealth, cardHealth, loggerHealth)
                .map(statuses -> {
                    Map<String, HealthStatus> downstreamServices = Map.of(
                            SWITCH_SERVICE_NAME, statuses.getT1(),
                            AUTH_SERVICE_NAME, statuses.getT2(),
                            CARD_MGMT_SERVICE_NAME, statuses.getT3(),
                            LOGGER_SERVICE_NAME, statuses.getT4());

                    HealthStatus gatewayStatus = downstreamServices.containsValue(HealthStatus.UNAVAILABLE)
                            ? HealthStatus.DEGRADED : HealthStatus.OK;

                    return new HealthResponse(
                            gatewayStatus,
                            GATEWAY_SERVICE_NAME,
                            smpGatewayProperties.getVersion(),
                            downstreamServices);
                });
    }
}
