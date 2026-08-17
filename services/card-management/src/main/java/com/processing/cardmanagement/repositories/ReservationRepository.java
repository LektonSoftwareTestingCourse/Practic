package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.Reservation;

import java.util.Optional;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    boolean isUnique(String rrn, String pan);

    Optional<Reservation> findByRrnAndPanForUpdate(String rrn, String pan);
}
