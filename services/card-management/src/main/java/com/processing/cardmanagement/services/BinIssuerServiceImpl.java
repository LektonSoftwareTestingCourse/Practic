package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.BinAlreadyExistException;
import com.processing.cardmanagement.exceptions.BinNotFoundException;
import com.processing.cardmanagement.models.BinIssuer;
import com.processing.cardmanagement.repositories.BinIssuerRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BinIssuerServiceImpl implements BinIssuerService {

    private final BinIssuerRepository repository;

    @Override
    public String getIssuerId(String bin) {
        return repository.findByBin(bin)
                .map(BinIssuer::issuerId)
                .orElseThrow(() -> new BinNotFoundException("Unknown BIN: " + bin));
    }

    @Override
    public List<BinIssuer> getAll() {
        return repository.findAll();
    }

    @Override
    public BinIssuer create(String bin, String issuerId) {
        if (repository.existsByBin(bin)) {
            throw new BinAlreadyExistException(bin);
        }
        return repository.save(new BinIssuer(bin, issuerId));
    }

    @Override
    public void delete(String bin) {
        if (!repository.existsByBin(bin)) {
            throw new BinNotFoundException(bin);
        }
        repository.deleteByBin(bin);
    }
}
