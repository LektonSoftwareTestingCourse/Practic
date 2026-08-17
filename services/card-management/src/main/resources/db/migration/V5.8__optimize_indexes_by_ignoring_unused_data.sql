DROP INDEX uk_cards_pan;
DROP INDEX idx_cards_bin_status_issuer_id_created_at;
DROP INDEX idx_cards_created_at;
DROP INDEX idx_outbox_status_retry;

CREATE UNIQUE INDEX uk_cards_pan ON cards (pan) WHERE cards.status != 'DELETED';
CREATE INDEX uk_bins_pan ON cards (bin) WHERE cards.status != 'DELETED';
CREATE INDEX idx_cards_bin_status_issuer_id_created_at
    ON cards (bin, status, issuer_id, created_at) WHERE cards.status != 'DELETED';
CREATE INDEX idx_outbox_status_retry
    ON outbox_event (status, retry_count) WHERE status = 'PENDING';
