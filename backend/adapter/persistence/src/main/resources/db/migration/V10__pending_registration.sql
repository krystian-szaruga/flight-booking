CREATE TABLE pending_registration (
    email         TEXT PRIMARY KEY,
    password_hash TEXT        NOT NULL,
    code_hash     TEXT        NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL,
    attempts      INT         NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    sent_at       TIMESTAMPTZ NOT NULL,
    send_count    INT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_pending_registration_email_lowercase CHECK (email = lower(email))
);

CREATE INDEX idx_pending_registration_expiry ON pending_registration (expires_at);

DELETE FROM reservation
 WHERE user_id IN (SELECT id FROM users WHERE email_verified_at IS NULL);

DELETE FROM users WHERE email_verified_at IS NULL;

ALTER TABLE users
    DROP COLUMN verification_code_hash,
    DROP COLUMN verification_expires_at,
    DROP COLUMN verification_attempts,
    DROP COLUMN verification_sent_at,
    DROP COLUMN verification_send_count;
