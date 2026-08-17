package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.BinIssuer;

import java.util.List;
import java.util.Optional;

public interface BinIssuerRepository {
    Optional<BinIssuer> findByBin(String bin);

    List<BinIssuer> findAll();

    BinIssuer save(BinIssuer binIssuer);

    void deleteByBin(String bin);

    boolean existsByBin(String bin);
}
