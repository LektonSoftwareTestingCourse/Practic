package com.processing.cardmanagement.mappers;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.processing.cardmanagement.events.*;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CardsBatchGeneratedEvent.class, name = "CARDS_BATCH_GENERATED"),
    @JsonSubTypes.Type(value = CardServiceCreationEvent.class, name = "CARD_SERVICE_CREATION"),
    @JsonSubTypes.Type(value = CardServiceDeletionEvent.class, name = "CARD_SERVICE_DELETION"),
    @JsonSubTypes.Type(value = CardServicePatchEvent.class, name = "CARD_SERVICE_PATCH"),
    @JsonSubTypes.Type(value = CardServiceReserveEvent.class, name = "CARD_SERVICE_RESERVE"),
    @JsonSubTypes.Type(value = CardServiceRollbackEvent.class, name = "CARD_SERVICE_ROLLBACK"),
    @JsonSubTypes.Type(value = CardServiceBulkUpdateEvent.class, name = "CARD_SERVICE_BULK_UPDATE")
})
public interface CardOutboxEventMixIn {}
