package com.processing.cardmanagement.services;

import com.processing.cardmanagement.models.CardOutboxEventData;
import com.processing.cardmanagement.options.OutboxOptions;
import com.processing.cardmanagement.repositories.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final OutboxEventProcessorImpl eventProcessor;
    private final OutboxOptions outboxOptions;

    @Scheduled(fixedDelayString = "${app.outbox.interval-ms}")
    @Transactional
    public void process() {
        List<CardOutboxEventData> events = outboxRepository.findPending(outboxOptions.maxRetryCount());
        events.forEach(eventProcessor::processSingleEvent);
    }
}
