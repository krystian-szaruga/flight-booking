CREATE TABLE revoked_token (
    token_id   UUID PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_revoked_token_expiry ON revoked_token (expires_at);
