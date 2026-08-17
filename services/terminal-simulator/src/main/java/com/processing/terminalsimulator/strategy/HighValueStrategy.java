package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.terminalsimulator.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class HighValueStrategy implements TransactionStrategy {
    @Override
    public TransactionType getType() {
        return TransactionType.HIGH_VALUE;
    }
    @Override
    public BigDecimal calculateAmount(CardModel card) {
        long amount = ThreadLocalRandom.current().nextLong(10_000_000L, 50_000_001L);
        return BigDecimal.valueOf(amount);
    }
    @Override
    public String getMcc() {
        return new String[]{"5411", "5812", "5814", "5732", "5399", "4814",
                "7994", "3501"}[ThreadLocalRandom.current().nextInt(8)];
    }
    @Override
    public boolean isInvalidPan() {
        return false;
    }
}
