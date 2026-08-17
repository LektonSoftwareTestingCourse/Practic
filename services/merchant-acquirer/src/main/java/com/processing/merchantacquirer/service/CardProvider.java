package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.GatewayClient;
import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.client.dto.CardsRequest;
import com.processing.merchantacquirer.client.dto.CardsResponse;
import java.util.List;

import com.processing.merchantacquirer.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardProvider {
  private final GatewayClient gatewayClient;

  public List<CardDataResponse> getCards(int count) {
    CardsRequest cardsRequest = new CardsRequest(count, 0, "ACTIVE", null);
    CardsResponse cardsResponse = gatewayClient.getCards(cardsRequest);

    if (cardsResponse.cards().isEmpty()) {
      throw new ResourceNotFoundException("Cards not found");
    }

    return cardsResponse.cards();
  }
}
