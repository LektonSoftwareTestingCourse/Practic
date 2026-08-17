package com.processing.cardmanagement.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.events.CardOutboxEvent;
import com.processing.cardmanagement.exceptions.OutboxDeserializationException;
import com.processing.cardmanagement.exceptions.OutboxSerializationException;
import com.processing.cardmanagement.models.CardOutboxEventData;
import com.processing.cardmanagement.models.CardOutboxEventDataEntity;
import org.springframework.stereotype.Component;

@Component
public class CardOutboxEventDataPersistenceMapper {

    private final ObjectMapper objectMapper;

    public CardOutboxEventDataPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.addMixIn(CardOutboxEvent.class, CardOutboxEventMixIn.class);
    }

    public CardOutboxEventDataEntity toEntity(CardOutboxEventData domain) {
        try {
            String payloadJson = objectMapper.writeValueAsString(domain.event());
            String eventType = domain.event().getClass().getSimpleName();

            return new CardOutboxEventDataEntity(
                domain.id(),
                eventType,
                payloadJson,
                domain.createdAt(),
                domain.processedAt(),
                domain.retryCount(),
                domain.lastError(),
                domain.status()
            );
        } catch (JsonProcessingException ignored) {
            throw new OutboxSerializationException(domain.event().getClass().getSimpleName());
        }
    }

    public CardOutboxEventData toDomain(CardOutboxEventDataEntity entity) {
        try {
            CardOutboxEvent event = objectMapper.readValue(entity.getPayload(), CardOutboxEvent.class);

            return new CardOutboxEventData(
                entity.getId(),
                event,
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                entity.getRetryCount(),
                entity.getLastError(),
                entity.getStatus()
            );
        } catch (JsonProcessingException e) {
            throw new OutboxDeserializationException(entity.getEventType());
        }
    }
}
