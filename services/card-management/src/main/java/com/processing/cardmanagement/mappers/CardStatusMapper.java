package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.CardStatus;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;

@Mapper(componentModel = "spring")
public interface CardStatusMapper {

    @ValueMapping(source = "DELETED", target = MappingConstants.THROW_EXCEPTION)
    CardModelStatus toCardModelStatus(CardStatus status);

    CardStatus toCardStatus(CardModelStatus status);
}
