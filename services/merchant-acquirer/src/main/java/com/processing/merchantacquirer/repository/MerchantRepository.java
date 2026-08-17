package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.domain.entity.Merchant;

import com.processing.merchantacquirer.domain.repository.MerchantRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String>, MerchantRepositoryPort {
}
