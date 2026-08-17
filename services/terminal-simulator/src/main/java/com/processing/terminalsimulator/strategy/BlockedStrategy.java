package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.terminalsimulator.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class BlockedStrategy implements TransactionStrategy {
    @Override
    public TransactionType getType() {
        return TransactionType.BLOCKED;
    }
    @Override
    public BigDecimal calculateAmount(CardModel card) {
        long randomAmount = ThreadLocalRandom.current().nextLong(10_000L, 500_001L);
        return BigDecimal.valueOf(randomAmount);
    }
    @Override
    public String getMcc() {
        return new String[]{"5411", "5812", "5814", "5732", "5399", "4814",
                "7994", "3501"}[ThreadLocalRandom.current().nextInt(8)];
    }
    @Override
    public CardModelStatus getRequiredCardStatus() {
        return CardModelStatus.BLOCKED;
    }
    @Override
    public boolean isInvalidPan() {
        return false;
    }
}
