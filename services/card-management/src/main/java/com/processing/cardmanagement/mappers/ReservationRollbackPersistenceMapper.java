package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.ReservationRollback;
import com.processing.cardmanagement.models.ReservationRollbackEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationRollbackPersistenceMapper {

    @Mapping(target = "reservation.id", source = "reservationId")
    ReservationRollbackEntity toEntity(ReservationRollback rollback);

    @Mapping(target = "reservationId", source = "reservation.id")
    ReservationRollback toDomain(ReservationRollbackEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservation", ignore = true)
    @Mapping(target = "pan", ignore = true)
    @Mapping(target = "rrn", ignore = true)
    void updateEntityFromDomain(
        ReservationRollback rollback,
        @MappingTarget ReservationRollbackEntity entity
    );
}
