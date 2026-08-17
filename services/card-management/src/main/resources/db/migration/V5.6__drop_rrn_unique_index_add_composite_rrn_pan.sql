DROP INDEX uk_reservations_rrn;
DROP INDEX uk_reservation_rollbacks_rrn;
CREATE UNIQUE INDEX uk_reservations_rrn_pan ON reservations (rrn, pan);
