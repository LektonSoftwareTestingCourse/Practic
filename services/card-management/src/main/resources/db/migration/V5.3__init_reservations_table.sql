CREATE TABLE reservations
(
    id                 UUID        NOT NULL PRIMARY KEY,
    pan                VARCHAR(16) NOT NULL,
    reservation_amount NUMERIC(19) NOT NULL,
    rrn                VARCHAR(12) NOT NULL,
    status             VARCHAR(20) NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_reservations_rrn ON reservations (rrn);
