CREATE TABLE refresh_token (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash TEXT        NOT NULL UNIQUE,
    family_id  UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_token_family ON refresh_token (family_id);
CREATE INDEX idx_refresh_token_expiry ON refresh_token (expires_at);
