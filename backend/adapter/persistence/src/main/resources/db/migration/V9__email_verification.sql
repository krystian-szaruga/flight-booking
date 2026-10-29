ALTER TABLE users
    ADD COLUMN email_verified_at        TIMESTAMPTZ NULL,
    ADD COLUMN verification_code_hash   TEXT        NULL,
    ADD COLUMN verification_expires_at  TIMESTAMPTZ NULL,
    ADD COLUMN verification_attempts    INT         NOT NULL DEFAULT 0,
    ADD COLUMN verification_sent_at     TIMESTAMPTZ NULL,
    ADD COLUMN verification_send_count  INT         NOT NULL DEFAULT 0;
