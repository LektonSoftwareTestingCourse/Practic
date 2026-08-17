package com.processing.cardmanagement.metrics;

import com.processing.cardmanagement.events.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class CardMetricsEventListener implements CardEventListener {

    private final MeterRegistry meterRegistry;
    private final AtomicLong cardsTotalGauge;

    public CardMetricsEventListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.cardsTotalGauge = meterRegistry.gauge("cards.total", new AtomicLong(0));
    }

    @Override
    public void onEvent(CardEvent event) {
        switch (event) {
            case CardServiceCreationEvent e -> meterRegistry
                .counter("cards.operations", "type", "created")
                .increment(e.amount());

            case CardServicePatchEvent ignored -> meterRegistry
                .counter("cards.operations", "type", "patched")
                .increment();

            case CardServiceDeletionEvent ignored -> meterRegistry
                .counter("cards.operations", "type", "deleted")
                .increment();

            case CardServiceReserveEvent ignored -> meterRegistry
                .counter("cards.operations", "type", "reserved")
                .increment();

            case CardServiceRollbackEvent ignored -> meterRegistry
                .counter("cards.operations", "type", "rolled_back")
                .increment();

            case CardsBatchGeneratedEvent e -> e.statusCount().forEach((status, count) ->
                meterRegistry.counter("cards.generated", "status", status.name())
                    .increment(count));

            case CardServiceBulkUpdateEvent e -> meterRegistry
                .counter("cards.operation", "type", "bulk_update")
                .increment(e.count());

            case CardsAmountUpdatedEvent e -> cardsTotalGauge.set(e.amount());
        }
    }
}
