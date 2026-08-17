package com.processing.cardmanagement.services;

import com.processing.cardmanagement.models.BinIssuer;

import java.util.List;

public interface BinIssuerService {
    String getIssuerId(String bin);

    List<BinIssuer> getAll();

    BinIssuer create(String bin, String issuerId);

    void delete(String bin);
}
