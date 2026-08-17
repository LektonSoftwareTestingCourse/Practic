package com.processing.terminalsimulator.client;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.cardmanagement.GetCardsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class GatewayClient {
    private final RestClient rest;
    private final String gatewayUrl;
    private final String cardManagementUrl;

    public GatewayClient(RestClient rest,
                         @Value("${gateway.url}") String gatewayUrl,
                         @Value("${gateway.card-management-url}") String cardManagementUrl) {
        this.rest = rest;
        this.gatewayUrl = gatewayUrl;
        this.cardManagementUrl = cardManagementUrl;
    }

    public AuthorizationResponse sendToGateway(AuthorizationRequest tx) {
        return rest.post().uri(gatewayUrl + "/api/transactions").body(tx)
                .retrieve().body(AuthorizationResponse.class);
    }

    public List<CardModel> getCardsFromCardManager(CardModelStatus status, int amount) {
        String url = cardManagementUrl + "/api/cards";
        String fullUrl = UriComponentsBuilder.fromUriString(url)
                .queryParam("status", status)
                .queryParam("limit", amount != 0 ? amount : null)
                .build()
                .toUriString();
        GetCardsResponse resp = rest.get().uri(fullUrl).retrieve().body(GetCardsResponse.class);
        if (resp == null || resp.total() == 0 || resp.cards().isEmpty()) {
            throw new IllegalStateException("No " + status + " cards available");
        }
        return resp.cards();
    }

}
