package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.Reservation;
import com.processing.cardmanagement.models.ReservationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationPersistenceMapper {

    ReservationEntity toEntity(Reservation reservation);

    @Mapping(target = "rolledBack", ignore = true)
    Reservation toDomain(ReservationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pan", ignore = true)
    @Mapping(target = "rrn", ignore = true)
    void updateEntityFromDomain(
        Reservation reservation,
        @MappingTarget ReservationEntity entity
    );
}
