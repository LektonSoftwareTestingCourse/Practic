package com.processing.merchantacquirer.domain.repository;

import com.processing.merchantacquirer.domain.entity.Merchant;

import java.util.Collection;
import java.util.List;

public interface MerchantRepositoryPort {
    List<Merchant> findByMccIn(Collection<String> mccCodes);
    List<Merchant> findAll();
    long count();
}
