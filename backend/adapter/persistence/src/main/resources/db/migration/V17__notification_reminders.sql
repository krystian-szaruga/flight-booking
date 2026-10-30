ALTER TABLE reservation
    ADD COLUMN payment_reminder_sent_at TIMESTAMPTZ,
    ADD COLUMN departure_reminder_sent_at TIMESTAMPTZ;

CREATE INDEX idx_reservation_payment_reminder
    ON reservation (expires_at)
    WHERE status = 'PENDING' AND payment_reminder_sent_at IS NULL;

CREATE INDEX idx_reservation_departure_reminder
    ON reservation (flight_id)
    WHERE status = 'CONFIRMED' AND departure_reminder_sent_at IS NULL;
