-- V1__create_notifications_table.sql
-- Notification Service: stores card events received from RabbitMQ

CREATE TABLE IF NOT EXISTS card_notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type   VARCHAR(100)  NOT NULL,
    routing_key  VARCHAR(100)  NOT NULL,
    payload      JSONB         NOT NULL,
    received_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_received_at
    ON card_notifications (received_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_event_type
    ON card_notifications (event_type);
