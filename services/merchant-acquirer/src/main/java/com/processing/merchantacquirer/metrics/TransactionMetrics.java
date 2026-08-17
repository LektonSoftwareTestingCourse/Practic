package com.processing.merchantacquirer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionMetrics {
    private final MeterRegistry meterRegistry;

    public void record(String mcc, String status) {
        Counter.builder("simulator.transactions")
                .tag("mcc", mcc)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }
}
