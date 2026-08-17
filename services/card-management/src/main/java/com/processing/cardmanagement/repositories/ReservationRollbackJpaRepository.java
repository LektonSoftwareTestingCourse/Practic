package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.ReservationRollbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReservationRollbackJpaRepository
    extends JpaRepository<ReservationRollbackEntity, UUID> {}
