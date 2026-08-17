package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.terminalsimulator.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AlmostDailyLimitStrategy implements TransactionStrategy {
    @Override
    public TransactionType getType() {
        return TransactionType.ALMOST_DAILY_LIMIT;
    }
    @Override
    public BigDecimal calculateAmount(CardModel card) {
        BigDecimal dailyLimit = card.dailyLimit();
        if (dailyLimit.compareTo(BigDecimal.ONE) <= 0) {
            return BigDecimal.ONE;
        }
        return dailyLimit.subtract(BigDecimal.ONE);
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
