CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);

CREATE INDEX idx_users_email ON users (email);
