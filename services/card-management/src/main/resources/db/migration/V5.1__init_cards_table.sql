CREATE TABLE cards
(
    id                UUID         NOT NULL PRIMARY KEY,
    pan               VARCHAR(16)  NOT NULL,
    bin               VARCHAR(6)   NOT NULL,
    cardholder_name   VARCHAR(255) NOT NULL,
    expiry_date       VARCHAR(4)   NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    currency_code     VARCHAR(3)   NOT NULL,
    daily_limit       NUMERIC(19)  NOT NULL,
    monthly_limit     NUMERIC(19)  NOT NULL,
    available_balance NUMERIC(19)  NOT NULL,
    issuer_id         VARCHAR(10)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_cards_pan ON cards (pan);
CREATE INDEX idx_cards_issuer_id_created_at ON cards (issuer_id, created_at);
CREATE INDEX idx_cards_created_at ON cards (created_at);
