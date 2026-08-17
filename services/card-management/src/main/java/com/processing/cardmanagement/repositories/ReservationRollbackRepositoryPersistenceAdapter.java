package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.mappers.ReservationRollbackPersistenceMapper;
import com.processing.cardmanagement.models.ReservationRollback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationRollbackRepositoryPersistenceAdapter
    implements ReservationRollbackRepository {

    private final ReservationRollbackPersistenceMapper mapper;
    private final ReservationRollbackJpaRepository jpaRepository;

    @Override
    public ReservationRollback save(ReservationRollback rollback) {
        return mapper.toDomain(
            jpaRepository.save(mapper.toEntity(rollback))
        );
    }
}
