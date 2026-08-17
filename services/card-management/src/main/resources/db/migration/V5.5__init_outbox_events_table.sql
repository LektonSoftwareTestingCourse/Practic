CREATE TABLE outbox_event
(
    id           UUID PRIMARY KEY,
    event_type   VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    retry_count  INT          NOT NULL DEFAULT 0,
    last_error   TEXT,
    status       VARCHAR(30)  NOT NULL
);

CREATE INDEX idx_outbox_status_retry ON outbox_event (status, retry_count);
