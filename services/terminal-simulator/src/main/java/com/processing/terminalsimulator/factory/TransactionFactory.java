package com.processing.terminalsimulator.factory;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.terminalsimulator.TerminalType;
import com.processing.terminalsimulator.model.PartofDay;
import com.processing.common.dto.terminalsimulator.TransactionType;
import com.processing.terminalsimulator.strategy.TransactionStrategy;
import com.processing.terminalsimulator.util.DateTimeGenerator;
import com.processing.terminalsimulator.util.StanGenerator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TransactionFactory {
    private final Map<TransactionType, TransactionStrategy> strategies;
    private final DateTimeGenerator dateTimeGenerator;
    private final StanGenerator stanGenerator;

    public TransactionFactory(List<TransactionStrategy> strategyList, StanGenerator stanGenerator,
                              DateTimeGenerator dateTimeGenerator) {
        this.stanGenerator = stanGenerator;
        this.dateTimeGenerator = dateTimeGenerator;

        this.strategies = new EnumMap<>(TransactionType.class);
        for (TransactionStrategy strategy : strategyList) {
            this.strategies.put(strategy.getType(), strategy);
        }
    }

    public AuthorizationRequest create(TransactionType transactionType, PartofDay partOfDay, CardModel card,
                                       String terminalId) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        TransactionStrategy transactionStrategy = strategies.get(transactionType);
        BigDecimal amount = transactionStrategy.calculateAmount(card);
        String mcc = transactionStrategy.getMcc();
        String pan = transactionStrategy.isInvalidPan() ? getInvalidPan(card) : card.pan();
        String stan = stanGenerator.getNextStan(terminalId);
        TerminalType[] types = TerminalType.values();
        String terminalType = types[random.nextInt(types.length)].name();
        String merchantId = String.format("MERCH%10d", random.nextLong(1, 10_000_000_000L));
        String acquirerId = String.format("ACQ%03d", random.nextLong(1, 1000));

        return AuthorizationRequest.builder()
                .mti("0100")
                .stan(stan)
                .pan(pan)
                .processingCode("000000")
                .amount(amount)
                .currencyCode(card.currencyCode())
                .transmissionDateTime(dateTimeGenerator.generate(partOfDay))
                .terminalId(terminalId)
                .terminalType(terminalType)
                .merchantId(merchantId)
                .mcc(mcc)
                .acquirerId(acquirerId)
                .issuerId("")
                .build();
    }

    public CardModelStatus getRequiredStatus(TransactionType type) {
        return strategies.get(type).getRequiredCardStatus();
    }

    private String getInvalidPan(CardModel card) {
        String validPan = card.pan();
        char last = validPan.charAt(validPan.length() - 1);
        char newLast = (last == '0') ? '1' : '0';
        return validPan.substring(0, validPan.length() - 1) + newLast;
    }
}
