package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.CardOutboxEventData;
import com.processing.cardmanagement.models.OutboxEventDataStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository {

    CardOutboxEventData save(CardOutboxEventData event);

    Optional<CardOutboxEventData> findById(UUID id);

    List<CardOutboxEventData> findPending(int maxRetry);

    List<CardOutboxEventData> findFailed();

    long countByStatus(OutboxEventDataStatus status);
}
