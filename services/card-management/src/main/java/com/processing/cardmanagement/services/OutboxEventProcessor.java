package com.processing.cardmanagement.services;

import com.processing.cardmanagement.models.CardOutboxEventData;

public interface OutboxEventProcessor {

    CardOutboxEventData save(CardOutboxEventData data);

    void processSingleEvent(CardOutboxEventData outboxEvent);
}
