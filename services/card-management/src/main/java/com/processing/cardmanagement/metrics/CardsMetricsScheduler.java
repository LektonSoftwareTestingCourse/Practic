package com.processing.cardmanagement.metrics;

import com.processing.cardmanagement.services.CardService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardsMetricsScheduler {

    private final CardService cardService;

    @Value("${app.metrics.scheduler.enabled:true}")
    private boolean isEnabled;

    @PostConstruct
    public void init() {
        updateCardsAmount();
    }

    @Scheduled(fixedDelayString = "${app.metrics.scheduler.update-rate-ms}")
    public void updateCardsAmount() {
        if (!isEnabled) {
            return;
        }
        cardService.countAllCardsAndUpdate();
    }
}
