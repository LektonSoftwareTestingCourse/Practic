package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEvent;
import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.events.CardsBatchGeneratedEvent;
import com.processing.cardmanagement.exceptions.CardGenerationLimitException;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.options.CardGeneratorOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardGeneratorServiceTest {

    private static final List<String> BINS = List.of(
            "400000",
            "400001",
            "400002",
            "400003",
            "400004"
    );

    private static final CardGeneratorOptions OPTIONS = new CardGeneratorOptions(
            new BigDecimal("1000"),
            new BigDecimal("2000"),
            new BigDecimal("100"),
            new BigDecimal("200"),
            "643",
            100
    );

    @Mock
    private CardService cardService;
    @Mock
    private CardEventNotifier eventNotifier;

    private CardGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        generatorService = new CardGeneratorService(cardService, OPTIONS, eventNotifier);
    }

    @Test
    void shouldGenerateCardsWithExpectedStatusDistributionWhenCountIsAllowed() {
        when(cardService.createCards(anyList())).thenReturn(List.of());

        generatorService.generate(100, BINS);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CardDraft>> draftsCaptor = ArgumentCaptor.forClass(List.class);
        verify(cardService).createCards(draftsCaptor.capture());

        List<CardDraft> drafts = draftsCaptor.getValue();
        assertThat(drafts).hasSize(100);

        Map<CardStatus, Long> statusCounts = drafts.stream()
                .collect(Collectors.groupingBy(CardDraft::status, Collectors.counting()));
        assertThat(statusCounts).containsEntry(CardStatus.ACTIVE, 95L);
        assertThat(statusCounts).containsEntry(CardStatus.INACTIVE, 3L);
        assertThat(statusCounts).containsEntry(CardStatus.BLOCKED, 2L);

        Map<String, Long> binCounts = drafts.stream()
                .collect(Collectors.groupingBy(CardDraft::bin, Collectors.counting()));
        assertThat(binCounts)
                .containsEntry("400000", 20L)
                .containsEntry("400001", 20L)
                .containsEntry("400002", 20L)
                .containsEntry("400003", 20L)
                .containsEntry("400004", 20L);
    }

    @Test
    void shouldPublishStatusCountsWhenGenerationCompletes() {
        when(cardService.createCards(anyList())).thenReturn(List.of());

        generatorService.generate(100, BINS);

        ArgumentCaptor<CardEvent> eventCaptor = ArgumentCaptor.forClass(CardEvent.class);
        verify(eventNotifier).notifyListeners(eventCaptor.capture());

        CardsBatchGeneratedEvent event = (CardsBatchGeneratedEvent) eventCaptor.getValue();
        assertThat(event.statusCount()).containsEntry(CardStatus.ACTIVE, 95L);
        assertThat(event.statusCount()).containsEntry(CardStatus.INACTIVE, 3L);
        assertThat(event.statusCount()).containsEntry(CardStatus.BLOCKED, 2L);
    }

    @Test
    void shouldRejectGenerationWhenCountExceedsLimit() {
        assertThatThrownBy(() -> generatorService.generate(101, BINS))
                .isInstanceOf(CardGenerationLimitException.class)
                .hasMessageContaining("100");

        verifyNoInteractions(cardService);
    }
}
