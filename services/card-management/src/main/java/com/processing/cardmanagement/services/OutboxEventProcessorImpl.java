package com.processing.cardmanagement.services;

import com.processing.cardmanagement.config.RabbitMQConfig;
import com.processing.cardmanagement.models.CardOutboxEventData;
import com.processing.cardmanagement.options.OutboxOptions;
import com.processing.cardmanagement.repositories.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
public class OutboxEventProcessorImpl implements OutboxEventProcessor {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxOptions outboxOptions;

    public OutboxEventProcessorImpl(
            OutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate,
            OutboxOptions outboxOptions) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.outboxOptions = outboxOptions;
    }

    @Override
    public CardOutboxEventData save(CardOutboxEventData data) {
        return outboxRepository.save(data);
    }

    @Override
    public void processSingleEvent(CardOutboxEventData outboxEventData) {
        try {
            var event = outboxEventData.event();
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CARD_EVENTS_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_PREFIX + event.getClass().getSimpleName(),
                event
            );
            outboxEventData = outboxEventData.processed();
        } catch (Exception e) {
            log.error("processing failed for event {}: {}", outboxEventData.id(), e.getMessage());
            outboxEventData = handleFail(outboxEventData, e);
        }
        outboxRepository.save(outboxEventData);
    }

    private CardOutboxEventData handleFail(CardOutboxEventData eventData, Exception ex) {
        eventData = eventData.withRetry(ex.getMessage());
        if (eventData.retryCount() >= outboxOptions.maxRetryCount()) {
            eventData = eventData.failed();
        }
        return eventData;
    }
}
