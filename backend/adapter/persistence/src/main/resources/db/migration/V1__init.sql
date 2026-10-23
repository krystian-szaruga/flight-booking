-- V1: catalogue (airport, flight, fare_class, flight_seat)
-- Owner: Flyway. Hibernate runs with ddl-auto: validate against this schema.

CREATE TABLE airport (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    iata_code     CHAR(3)     NOT NULL,
    name          TEXT        NOT NULL,
    city          TEXT        NOT NULL,
    country_code  CHAR(2)     NOT NULL,
    CONSTRAINT uq_airport_iata UNIQUE (iata_code),
    CONSTRAINT ck_airport_iata_upper CHECK (iata_code = upper(iata_code)),
    CONSTRAINT ck_airport_country_upper CHECK (country_code = upper(country_code))
);

CREATE TABLE flight (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flight_number        TEXT NOT NULL,
    origin_airport_id    UUID NOT NULL REFERENCES airport (id),
    destination_airport_id UUID NOT NULL REFERENCES airport (id),
    departure_at         TIMESTAMPTZ NOT NULL,
    arrival_at           TIMESTAMPTZ NOT NULL,
    aircraft             TEXT NOT NULL,
    status               TEXT NOT NULL DEFAULT 'SCHEDULED',
    CONSTRAINT uq_flight_number_departure UNIQUE (flight_number, departure_at),
    CONSTRAINT ck_flight_arrival_after_departure CHECK (arrival_at > departure_at),
    CONSTRAINT ck_flight_distinct_airports CHECK (origin_airport_id <> destination_airport_id),
    CONSTRAINT ck_flight_status CHECK (status IN ('SCHEDULED', 'DELAYED', 'CANCELLED'))
);

CREATE TABLE fare_class (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flight_id         UUID          NOT NULL REFERENCES flight (id) ON DELETE CASCADE,
    cabin             TEXT          NOT NULL,
    base_price_amount NUMERIC(12, 2) NOT NULL,
    currency          CHAR(3)       NOT NULL DEFAULT 'PLN',
    multiplier        NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    CONSTRAINT uq_fare_class_flight_cabin UNIQUE (flight_id, cabin),
    -- target for the composite FK from flight_seat (keeps fare_class on the same flight)
    CONSTRAINT uq_fare_class_id_flight UNIQUE (id, flight_id),
    CONSTRAINT ck_fare_class_cabin CHECK (cabin IN ('ECONOMY', 'PREMIUM_ECONOMY', 'BUSINESS', 'FIRST')),
    CONSTRAINT ck_fare_class_price_positive CHECK (base_price_amount > 0),
    CONSTRAINT ck_fare_class_multiplier_positive CHECK (multiplier > 0),
    CONSTRAINT ck_fare_class_currency CHECK (currency = upper(currency))
);

CREATE TABLE flight_seat (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flight_id       UUID   NOT NULL,
    fare_class_id   UUID   NOT NULL,
    seat_number     TEXT   NOT NULL,
    status          TEXT   NOT NULL DEFAULT 'AVAILABLE',
    version         BIGINT NOT NULL DEFAULT 0,
    hold_expires_at TIMESTAMPTZ,
    reservation_id  UUID,
    CONSTRAINT fk_flight_seat_flight
        FOREIGN KEY (flight_id) REFERENCES flight (id) ON DELETE CASCADE,
    CONSTRAINT fk_flight_seat_fare_class
        FOREIGN KEY (fare_class_id, flight_id) REFERENCES fare_class (id, flight_id) ON DELETE CASCADE,
    CONSTRAINT uq_flight_seat_flight_number UNIQUE (flight_id, seat_number),
    CONSTRAINT ck_flight_seat_number CHECK (seat_number ~ '^[0-9]{1,2}[A-F]$'),
    CONSTRAINT ck_flight_seat_status CHECK (status IN ('AVAILABLE', 'HELD', 'SOLD')),
    CONSTRAINT ck_flight_seat_hold_consistent CHECK (
        (status = 'HELD' AND hold_expires_at IS NOT NULL AND reservation_id IS NOT NULL)
        OR (status = 'SOLD' AND hold_expires_at IS NULL AND reservation_id IS NOT NULL)
        OR (status = 'AVAILABLE' AND hold_expires_at IS NULL AND reservation_id IS NULL)
    ),
    CONSTRAINT ck_flight_seat_version_non_negative CHECK (version >= 0)
);

-- Search: stable ordering by departure_at, then id; filter by route + date.
CREATE INDEX idx_flight_route_departure ON flight (origin_airport_id, destination_airport_id, departure_at, id);
CREATE INDEX idx_flight_departure ON flight (departure_at, id);

-- Availability lookups and the seat map rendering.
CREATE INDEX idx_flight_seat_flight_status ON flight_seat (flight_id, status, seat_number);
CREATE INDEX idx_flight_seat_fare_class ON flight_seat (fare_class_id);

-- Expired-hold sweeper: only HELD rows are candidates.
CREATE INDEX idx_flight_seat_hold_expiry ON flight_seat (hold_expires_at) WHERE status = 'HELD';
CREATE INDEX idx_flight_seat_reservation ON flight_seat (reservation_id) WHERE reservation_id IS NOT NULL;
