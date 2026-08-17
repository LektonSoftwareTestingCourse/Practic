DROP INDEX idx_cards_issuer_id_created_at;
CREATE INDEX idx_cards_bin_status_issuer_id_created_at
    ON cards (bin, status, issuer_id, created_at);
