package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CardExpiryDateMapper.class)
public interface CardPersistenceMapper {

    CardEntity toEntity(Card card);

    @Mapping(target = "withReservation", ignore = true)
    @Mapping(target = "withRollback", ignore = true)
    @Mapping(target = "copyWithPan", ignore = true)
    Card toDomain(CardEntity entity);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDomain(Card card, @MappingTarget CardEntity entity);
}
