package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.Card;
import com.processing.common.dto.cardmanagement.CardModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring", uses = CardStatusMapper.class)
public interface CardRestMapper {

    CardModel toDto(Card card);

    @Mapping(target = "withReservation", ignore = true)
    @Mapping(target = "withRollback", ignore = true)
    @Mapping(target = "copyWithPan", ignore = true)
    Card toDomain(CardModel model);

    default BigDecimal roundBalance(BigDecimal availableBalance) {
        if (availableBalance == null) {
            return null;
        }
        return availableBalance.setScale(0, RoundingMode.HALF_EVEN);
    }
}
