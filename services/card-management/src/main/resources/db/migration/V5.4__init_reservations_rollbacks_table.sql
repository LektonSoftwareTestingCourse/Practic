CREATE TABLE reservation_rollbacks
(
    id              UUID                     NOT NULL PRIMARY KEY,
    reservation_id  UUID                     NOT NULL REFERENCES reservations (id),
    pan             VARCHAR(16)              NOT NULL,
    rollback_amount NUMERIC(19)              NOT NULL,
    rrn             VARCHAR(12)              NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_reservation_rollbacks_rrn ON reservation_rollbacks (rrn);
CREATE UNIQUE INDEX uk_reservation_rollbacks_reservation_id ON reservation_rollbacks (reservation_id);
