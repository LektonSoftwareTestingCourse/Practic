package com.processing.cardmanagement.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.processing.common.utils.MaskPan.maskPan;

@Slf4j
@Component
public class CardServiceLogEventListener implements CardEventListener {

    @Override
    public void onEvent(CardEvent event) {
        switch (event) {
            case CardServiceCreationEvent e -> log.info("Created {} cards", e.amount());
            case CardServicePatchEvent e -> log.info("Patched card {}", maskPan(e.pan()));
            case CardServiceDeletionEvent e -> log.info("Deleted card {}", maskPan(e.pan()));
            case CardServiceReserveEvent e ->
                log.info("Reserved {} from card {} with RRN {}", e.amount(), maskPan(e.pan()), e.rrn());
            case CardServiceRollbackEvent e ->
                log.warn("Rolled back {} from card {} with RRN {}", e.amount(), maskPan(e.pan()), e.rrn());
            case CardsBatchGeneratedEvent e -> log.info("Generated {} cards: {}",
                e.statusCount().values().stream().mapToLong(Long::longValue).sum(), e.statusCount());
            case CardServiceBulkUpdateEvent e -> log.info("Bulk updated {} cards to status {}", e.count(), e.status());
            default -> {
            }
        }
    }
}
