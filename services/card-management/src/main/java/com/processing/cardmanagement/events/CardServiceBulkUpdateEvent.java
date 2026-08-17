package com.processing.cardmanagement.events;

import com.processing.cardmanagement.models.CardStatus;

public record CardServiceBulkUpdateEvent(
    int count,
    CardStatus status
) implements CardOutboxEvent {}
