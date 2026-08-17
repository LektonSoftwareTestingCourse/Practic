package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.events.CardsBatchGeneratedEvent;
import com.processing.cardmanagement.exceptions.CardGenerationLimitException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.options.CardGeneratorOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Сервис для генерации тестовых банковских карт
 * Карты равномерно распределяются по переданным BIN-префиксам
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardGeneratorService {

    private final CardService cardService;
    private final CardGeneratorOptions generatorOptions;
    private final CardEventNotifier eventNotifier;
    private final Faker faker = new Faker();

    private static final long DAYS_IN_MONTH = 30;

    /**
     * Генерирует указанное количество тестовых карт и сохраняет их в базе данных
     * Карты равномерно распределяются по BIN-ам
     * Распределение статусов: ACTIVE - 95%, INACTIVE - 3%, BLOCKED - 2%
     * Сохранение выполняется батчами. Размер батча задается в application.properties
     *
     * @param count количество карт для генерации
     * @param bins  список BIN-префиксов для распределения карт
     * @return список созданных карт
     */
    public List<Card> generate(int count, List<String> bins) {
        if (count > generatorOptions.maxCount()) {
            throw new CardGenerationLimitException(generatorOptions.maxCount());
        }

        Map<CardStatus, Long> statusCounts = new HashMap<>();
        List<CardStatus> cardStatuses = generateStatuses(count);

        log.info("Generating {} cards for bins: {}", count, bins);
        List<CardDraft> cards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bin = bins.get(i % bins.size());

            String cardholderName = faker.name().fullName().toUpperCase();

            BigDecimal balance = BigDecimal.valueOf(
                ThreadLocalRandom.current().nextLong(
                    generatorOptions.minBalance().longValue(),
                    generatorOptions.maxBalance().longValue()
                )
            );
            BigDecimal dailyLimit = BigDecimal.valueOf(
                ThreadLocalRandom.current().nextLong(
                    generatorOptions.minDailyLimit().longValue(),
                    generatorOptions.maxDailyLimit().longValue()
                )
            );
            BigDecimal monthlyLimit = BigDecimal.valueOf(
                dailyLimit.longValue() * DAYS_IN_MONTH
            );


            CardDraft card = new CardDraft(
                bin,
                cardholderName,
                cardStatuses.get(i),
                generatorOptions.currencyCode(),
                dailyLimit,
                monthlyLimit,
                balance
            );

            statusCounts.merge(card.status(), 1L, Long::sum);
            cards.add(card);
        }

        List<Card> result = cardService.createCards(cards);
        eventNotifier.notifyListeners(new CardsBatchGeneratedEvent(statusCounts));

        log.info("Successfully generated {} cards", count);
        return result;
    }

    private List<CardStatus> generateStatuses(int count) {
        List<CardStatus> cardStatuses = new ArrayList<>();

        int activeStatuses = count * 95 / 100;
        int inactiveStatuses = count * 3 / 100;
        int blockedStatuses = count - activeStatuses - inactiveStatuses;

        for (int i = 0; i < activeStatuses; i++) {
            cardStatuses.add(CardStatus.ACTIVE);
        }

        for (int i = 0; i < inactiveStatuses; i++) {
            cardStatuses.add(CardStatus.INACTIVE);
        }

        for (int i = 0; i < blockedStatuses; i++) {
            cardStatuses.add(CardStatus.BLOCKED);
        }

        Collections.shuffle(cardStatuses);

        return cardStatuses;
    }
}
