package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.ReservationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservationEntity> findWithPessimisticLockByRrnAndPan(String rrn, String pan);

    boolean existsByRrnAndPan(String rrn, String pan);
}
