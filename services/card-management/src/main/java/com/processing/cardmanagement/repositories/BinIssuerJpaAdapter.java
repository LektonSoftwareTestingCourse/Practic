package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.BinIssuer;
import com.processing.cardmanagement.models.BinIssuerEntity;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class BinIssuerJpaAdapter implements BinIssuerRepository {

    private final BinIssuerJpaRepository jpaRepository;

    @Override
    public Optional<BinIssuer> findByBin(String bin) {
        return jpaRepository.findById(bin)
                .map(this::toDomain);
    }

    @Override
    public List<BinIssuer> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public BinIssuer save(BinIssuer binIssuer) {
        return toDomain(jpaRepository.
                save(new BinIssuerEntity(binIssuer.bin(), binIssuer.issuerId())));
    }

    @Override
    public void deleteByBin(String bin) {
        jpaRepository.deleteById(bin);
    }

    @Override
    public boolean existsByBin(String bin) {
        return jpaRepository.existsById(bin);
    }

    private BinIssuer toDomain(BinIssuerEntity entity) {
        return new BinIssuer(entity.getBin(), entity.getIssuerId());
    }
}
