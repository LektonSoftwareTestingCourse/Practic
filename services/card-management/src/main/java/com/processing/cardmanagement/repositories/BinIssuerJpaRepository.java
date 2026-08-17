package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.BinIssuerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinIssuerJpaRepository extends JpaRepository<BinIssuerEntity, String> {
}
