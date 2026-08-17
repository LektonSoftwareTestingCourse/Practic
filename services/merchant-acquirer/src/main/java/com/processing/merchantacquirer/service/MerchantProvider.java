package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.model.Scenario;
import com.processing.merchantacquirer.domain.repository.MerchantRepositoryPort;
import com.processing.merchantacquirer.exception.ResourceNotFoundException;

import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantProvider {
  private final MerchantRepositoryPort merchantRepository;

  public List<Merchant> getMerchant(Collection<String> mccCodes, Scenario scenario) {
    Collection<String> effective = (mccCodes == null || mccCodes.isEmpty()) ? scenario.getMcc() : mccCodes;
    List<Merchant> merchants = merchantRepository.findByMccIn(effective);

    if (merchants.isEmpty()) {
      throw new ResourceNotFoundException("Merchants with given mcc (" + effective + ") not found");
    }

    return merchants;
  }

  public List<Merchant> getAll() {
    return merchantRepository.findAll();
  }

  public long count() {
    return merchantRepository.count();
  }
}
