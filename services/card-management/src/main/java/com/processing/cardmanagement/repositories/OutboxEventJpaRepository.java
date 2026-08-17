package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.CardOutboxEventDataEntity;
import com.processing.cardmanagement.models.OutboxEventDataStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<CardOutboxEventDataEntity, UUID> {

    List<CardOutboxEventDataEntity> findByStatusAndRetryCountLessThan(OutboxEventDataStatus status, int maxRetry);

    List<CardOutboxEventDataEntity> findByStatus(OutboxEventDataStatus status);

    long countByStatus(OutboxEventDataStatus status);
}
