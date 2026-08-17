package com.processing.terminalsimulator.service;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.terminalsimulator.TerminalScenario;
import com.processing.terminalsimulator.client.GatewayClient;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CardRegistry {

    private final Map<CardModelStatus, List<CardModel>> cardsByStatus;

    public CardRegistry(GatewayClient gatewayClient, TerminalScenario scenario, int totalCardsAmount) {
        List<CardModel> allCards = loadCards(gatewayClient, scenario, totalCardsAmount);
        this.cardsByStatus = allCards.stream().collect(Collectors.groupingBy(CardModel::status));
    }

    public CardModel getRandomCard(CardModelStatus cardStatus) {
        List<CardModel> filtered = cardsByStatus.get(cardStatus);
        if (filtered == null || filtered.isEmpty()) {
            throw new IllegalStateException("No " + cardStatus + " cards available");
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(filtered.size());
        return filtered.get(randomIndex);
    }

    private List<CardModel> loadCards(GatewayClient gatewayClient, TerminalScenario scenario, int cardsAmount) {
        boolean needBlocked = scenario == TerminalScenario.mixed || scenario == TerminalScenario.declines_test;
        int blockedPercent = 20; // 20%
        int activePercent = (needBlocked) ? 80 : 100;

        List<CardModel> activeCards = gatewayClient.getCardsFromCardManager(CardModelStatus.ACTIVE,
                (cardsAmount * activePercent / 100));
        if (activeCards == null || activeCards.isEmpty()) {
            throw new IllegalStateException("No ACTIVE cards available");
        }
        List<CardModel> newCards = new ArrayList<>(activeCards);
        if (needBlocked) {
            List<CardModel> blockedCards = gatewayClient.getCardsFromCardManager(CardModelStatus.BLOCKED,
                    (cardsAmount * blockedPercent / 100));
            if (blockedCards == null || blockedCards.isEmpty()) {
                throw new IllegalStateException("No BLOCKED cards available");
            }
            newCards.addAll(blockedCards);
        }
        return newCards;
    }
}
