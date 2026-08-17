package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.repository.TerminalRepositoryPort;
import com.processing.merchantacquirer.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class TerminalProvider {
    private final TerminalRepositoryPort terminalRepository;

    private final Map<String, List<Terminal>> terminalsByMerchants = new ConcurrentHashMap<>();

    public Terminal getByMerchant(String merchantId) {
        List<Terminal> terminals = terminalsByMerchants.computeIfAbsent(merchantId, terminalRepository::findByMerchantId);

        if (terminals.isEmpty()) {
            throw new ResourceNotFoundException("Merchant not have terminals, merchant id: " + merchantId);
        }
        return terminals.get(ThreadLocalRandom.current().nextInt(0, terminals.size()));
    }
}
