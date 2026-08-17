CREATE OR REPLACE FUNCTION normalize_decline_reason_fts(value TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT REPLACE(REPLACE(COALESCE(value, ''), '_', ' '), '-', ' ');
$$;

CREATE OR REPLACE FUNCTION fts_match_decline_reason(document TEXT, query TEXT)
RETURNS BOOLEAN
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT TO_TSVECTOR('simple', normalize_decline_reason_fts(document))
           @@ PLAINTO_TSQUERY('simple', normalize_decline_reason_fts(query));
$$;

CREATE INDEX IF NOT EXISTS idx_transactions_decline_reason_fts
    ON transactions
    USING gin (TO_TSVECTOR('simple', normalize_decline_reason_fts(decline_reason)));
