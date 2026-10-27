-- V2: provenance columns for ingested catalogue data

ALTER TABLE airport
    ADD COLUMN source     TEXT        NOT NULL DEFAULT 'manual',
    ADD COLUMN synced_at  TIMESTAMPTZ;

ALTER TABLE flight
    ADD COLUMN source     TEXT        NOT NULL DEFAULT 'manual',
    ADD COLUMN synced_at  TIMESTAMPTZ;

ALTER TABLE airport
    ADD CONSTRAINT ck_airport_source_not_blank CHECK (length(trim(source)) > 0);

ALTER TABLE flight
    ADD CONSTRAINT ck_flight_source_not_blank CHECK (length(trim(source)) > 0);