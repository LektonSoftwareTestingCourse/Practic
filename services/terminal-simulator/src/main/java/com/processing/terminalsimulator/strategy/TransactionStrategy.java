package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.terminalsimulator.TransactionType;

import java.math.BigDecimal;

public interface TransactionStrategy {
    TransactionType getType();
    BigDecimal calculateAmount(CardModel card);
    String getMcc();
    boolean isInvalidPan();
    default CardModelStatus getRequiredCardStatus() {
        return CardModelStatus.ACTIVE;
    }
}
