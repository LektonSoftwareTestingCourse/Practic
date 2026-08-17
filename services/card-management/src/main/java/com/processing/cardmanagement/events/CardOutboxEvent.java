package com.processing.cardmanagement.events;

public sealed interface CardOutboxEvent extends CardEvent permits
    CardServiceBulkUpdateEvent,
    CardServiceCreationEvent,
    CardServiceDeletionEvent,
    CardServicePatchEvent,
    CardServiceReserveEvent,
    CardServiceRollbackEvent,
    CardsBatchGeneratedEvent {}
