package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.ReservationRollback;

/**
 * Репозиторий для хранения ролбэков
 */
public interface ReservationRollbackRepository {

    ReservationRollback save(ReservationRollback rollback);
}
