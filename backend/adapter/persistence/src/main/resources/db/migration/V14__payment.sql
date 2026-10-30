ALTER TABLE reservation DROP CONSTRAINT ck_reservation_expiry_consistent;

ALTER TABLE reservation
    ADD CONSTRAINT ck_reservation_expiry_consistent CHECK (
        (status = 'PENDING' AND expires_at IS NOT NULL)
        OR (status IN ('CONFIRMED', 'EXPIRED', 'CANCELLED') AND expires_at IS NULL)
    );

CREATE TABLE payment (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id     UUID           NOT NULL REFERENCES reservation (id) ON DELETE CASCADE,
    idempotency_key    TEXT           NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    currency           CHAR(3)        NOT NULL,
    status             TEXT           NOT NULL,
    card_last4         CHAR(4)        NOT NULL,
    decline_reason     TEXT,
    provider_reference TEXT,
    created_at         TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_payment_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_payment_status CHECK (status IN ('APPROVED', 'DECLINED')),
    CONSTRAINT ck_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payment_currency CHECK (currency = upper(currency)),
    CONSTRAINT ck_payment_card_last4 CHECK (card_last4 ~ '^[0-9]{4}$'),
    CONSTRAINT ck_payment_decline_reason CHECK (status = 'DECLINED' OR decline_reason IS NULL),
    CONSTRAINT ck_payment_provider_reference CHECK (status = 'APPROVED' OR provider_reference IS NULL)
);

CREATE INDEX idx_payment_reservation ON payment (reservation_id);
