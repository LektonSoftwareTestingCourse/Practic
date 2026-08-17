package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.terminalsimulator.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class NormalStrategy implements TransactionStrategy {
    @Override
    public TransactionType getType() {
        return TransactionType.NORMAL;
    }
    @Override
    public BigDecimal calculateAmount(CardModel card) {
        long amount = ThreadLocalRandom.current().nextLong(10_000L, 500_001L);
        return BigDecimal.valueOf(amount);
    }
    @Override
    public String getMcc() {
        return "5411";
    }
    @Override
    public boolean isInvalidPan() {
        return false;
    }
}
