CREATE TABLE reservation (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flight_id  UUID        NOT NULL REFERENCES flight (id) ON DELETE CASCADE,
    user_id    UUID,
    status     TEXT        NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_reservation_status CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_reservation_expiry_consistent CHECK (
        (status IN ('PENDING', 'CONFIRMED') AND expires_at IS NOT NULL)
        OR (status IN ('EXPIRED', 'CANCELLED') AND expires_at IS NULL)
    )
);

CREATE TABLE reservation_seat (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL REFERENCES reservation (id) ON DELETE CASCADE,
    flight_seat_id UUID NOT NULL UNIQUE REFERENCES flight_seat (id),
    passenger_name TEXT NOT NULL,
    CONSTRAINT ck_reservation_seat_passenger_not_blank CHECK (length(trim(passenger_name)) > 0)
);

ALTER TABLE flight_seat
    ADD CONSTRAINT fk_flight_seat_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservation (id) ON DELETE SET NULL;

CREATE INDEX idx_reservation_expiry ON reservation (expires_at) WHERE status = 'PENDING';
CREATE INDEX idx_reservation_seat_rep ON reservation_seat (reservation_id);
