package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.CardEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardJpaRepository
    extends JpaRepository<CardEntity, UUID>,
    CardCriteriaBuilderJpaRepository {

    Optional<CardEntity> findByPan(String pan);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CardEntity> findWithPessimisticLockByPan(String pan);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CardEntity> findWithPessimisticLockByPanIn(List<String> pans);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CardEntity> findWithPessimisticLockByBinIn(List<String> bins);
}
