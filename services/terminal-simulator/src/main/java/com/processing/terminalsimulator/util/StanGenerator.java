package com.processing.terminalsimulator.util;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StanGenerator {
    private final Map<String, AtomicInteger> terminalStans = new ConcurrentHashMap<>();
    private static final int MAX_STAN_COUNT = 999_999;

    public String getNextStan(String terminalId) {
        AtomicInteger stanCounter = terminalStans.computeIfAbsent(terminalId, k -> new AtomicInteger(0));

        int stan = stanCounter.updateAndGet(current -> current >= MAX_STAN_COUNT ? 1 : current + 1);

        return String.format("%06d", stan);
    }

}
