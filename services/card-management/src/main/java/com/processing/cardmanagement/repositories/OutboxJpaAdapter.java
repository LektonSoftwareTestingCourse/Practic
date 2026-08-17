package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.mappers.CardOutboxEventDataPersistenceMapper;
import com.processing.cardmanagement.models.CardOutboxEventData;
import com.processing.cardmanagement.models.OutboxEventDataStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxJpaAdapter implements OutboxRepository {

    private final OutboxEventJpaRepository jpaRepository;
    private final CardOutboxEventDataPersistenceMapper persistenceMapper;

    @Override
    public CardOutboxEventData save(CardOutboxEventData event) {
        return persistenceMapper.toDomain(
            jpaRepository.save(persistenceMapper.toEntity(event))
        );
    }

    @Override
    public Optional<CardOutboxEventData> findById(UUID id) {
        return jpaRepository.findById(id).map(persistenceMapper::toDomain);
    }

    @Override
    public List<CardOutboxEventData> findPending(int maxRetry) {
        return jpaRepository
            .findByStatusAndRetryCountLessThan(OutboxEventDataStatus.PENDING, maxRetry)
            .stream()
            .map(persistenceMapper::toDomain)
            .toList();
    }

    @Override
    public List<CardOutboxEventData> findFailed() {
        return jpaRepository
            .findByStatus(OutboxEventDataStatus.FAILED)
            .stream()
            .map(persistenceMapper::toDomain)
            .toList();
    }

    @Override
    public long countByStatus(OutboxEventDataStatus status) {
        return jpaRepository.countByStatus(status);
    }
}
