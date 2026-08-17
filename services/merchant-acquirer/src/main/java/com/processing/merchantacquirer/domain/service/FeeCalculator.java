package com.processing.merchantacquirer.domain.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FeeCalculator {
    public BigDecimal calculate(BigDecimal fee, BigDecimal amount) {
        return amount
                    .multiply(fee)
                    .setScale(0, RoundingMode.HALF_EVEN);
    }
}
