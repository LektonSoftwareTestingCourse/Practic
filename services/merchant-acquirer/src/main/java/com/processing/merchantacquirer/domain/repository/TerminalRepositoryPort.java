package com.processing.merchantacquirer.domain.repository;

import com.processing.merchantacquirer.domain.entity.Terminal;

import java.util.List;

public interface TerminalRepositoryPort {
    List<Terminal> findByMerchantId(String merchantId);
}
