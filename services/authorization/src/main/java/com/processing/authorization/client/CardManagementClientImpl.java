package com.processing.authorization.client;

import com.processing.authorization.events.AuthorizationEventNotifier;
import com.processing.authorization.events.CmsClientGetCardEvent;
import com.processing.authorization.events.CmsClientReserveEvent;
import com.processing.authorization.events.CmsClientRollbackEvent;
import com.processing.authorization.exceptions.*;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.ReserveRequest;
import com.processing.common.utils.MaskPan;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@RequiredArgsConstructor
@Component
public class CardManagementClientImpl implements CardManagementClient {
    private final RestClient restClient;
    private final AuthorizationEventNotifier eventNotifier;

    @Value("${card-management.url}")
    private String cmsUrl;

    @Override
    public CardModel getCard(String pan) {
        URI uri = UriComponentsBuilder
                .fromUriString(cmsUrl)
                .scheme("http")
                .path("/api/cards/{pan}")
                .buildAndExpand(pan)
                .toUri();
        eventNotifier.notify(new CmsClientGetCardEvent(pan));
        return restClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(status -> status.value() == 500, (req, res) -> {
                    throw new InternalCardManagerException("Internal card management error");
                })
                .onStatus(status -> status.value() == 503, (req, res) -> {
                    throw new ServiceUnavailableException("Card Management service unavailable");
                })
                .onStatus(status -> status.value() == 400, (req, res) -> {
                    throw new InvalidGetCardRequestException("Invalid pan");
                })
                .onStatus(status -> status.value() == 402, (req, res) -> {
                    throw new PaymentRequiredException("Payment Required from card-management");
                })
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new CardNotFoundException("Card not found");
                })
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    throw new GetCardException("Failed to get card. Status: " + res.getStatusCode());
                })
                .body(CardModel.class);
    }

    @Override
    public void reserve(BigDecimal amount, String rrn, String pan) {
        ReserveRequest reserveRequest = new ReserveRequest(amount, rrn);
        URI uri = UriComponentsBuilder
                .fromUriString(cmsUrl)
                .scheme("http")
                .path("/api/cards/{pan}/reserve")
                .buildAndExpand(pan)
                .toUri();
        eventNotifier.notify(new CmsClientReserveEvent(pan));
        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(reserveRequest)
                .retrieve()
                .onStatus(status -> status.value() == 500, (req, res) -> {
                    throw new InternalCardManagerException("Internal card management error");
                })
                .onStatus(status -> status.value() == 503, (req, res) -> {
                    throw new ServiceUnavailableException("Card Management service unavailable");
                })
                .onStatus(status -> status.value() == 400, (req, res) -> {
                    throw new InvalidReserveRequestException("Invalid reserve request");
                })
                .onStatus(status -> status.value() == 402, (req, res) -> {
                    throw new InsufficientFundsException("Insufficient Funds from card-management");
                })
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new CardNotFoundException("Card not found");
                })
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    throw new ReserveException("Failed to reserve. Status: " + res.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public void rollback(RollbackRequest request) {
        URI uri = UriComponentsBuilder
                .fromUriString(cmsUrl)
                .scheme("http")
                .path("/api/cards/{pan}/rollback")
                .buildAndExpand(request.pan())
                .toUri();
        eventNotifier.notify(new CmsClientRollbackEvent(request.pan()));
        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(status -> status.value() == 500, (req, res) -> {
                    throw new InternalCardManagerException("Internal card management error");
                })
                .onStatus(status -> status.value() == 503, (req, res) -> {
                    throw new ServiceUnavailableException("Card Management service unavailable");
                })
                .onStatus(status -> status.value() == 400, (req, res) -> {
                    throw new InvalidRollbackRequestException("Invalid rollback request");
                })
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new CardNotFoundException("Card not found: " + MaskPan.maskPan(request.pan()));
                })
                .onStatus(status -> status.value() == 409, (req, res) -> {
                    throw new RollbackConflictException("Rollback conflict");
                })
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    throw new RollbackFailureException("Failed to rollback. Status: " + res.getStatusCode());
                })
                .toBodilessEntity();
    }
}
