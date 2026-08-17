package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.mappers.ReservationPersistenceMapper;
import com.processing.cardmanagement.models.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryPersistenceAdapter implements ReservationRepository {

    private final ReservationJpaRepository jpaRepository;
    private final ReservationPersistenceMapper mapper;

    @Override
    public Reservation save(Reservation reservation) {
        return mapper.toDomain(
            jpaRepository.save(mapper.toEntity(reservation))
        );
    }

    @Override
    public boolean isUnique(String rrn, String pan) {
        return !jpaRepository.existsByRrnAndPan(rrn, pan);
    }

    @Override
    public Optional<Reservation> findByRrnAndPanForUpdate(String rrn, String pan) {
        return jpaRepository.findWithPessimisticLockByRrnAndPan(rrn, pan).map(mapper::toDomain);
    }
}
