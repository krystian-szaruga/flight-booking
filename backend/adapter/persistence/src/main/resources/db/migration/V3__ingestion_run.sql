CREATE TABLE ingestion_run (
                               id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               source            TEXT        NOT NULL,
                               started_at        TIMESTAMPTZ NOT NULL,
                               finished_at       TIMESTAMPTZ,
                               status            TEXT        NOT NULL DEFAULT 'RUNNING',
                               airports_written  INTEGER     NOT NULL DEFAULT 0,
                               flights_written   INTEGER     NOT NULL DEFAULT 0,
                               rejected          INTEGER     NOT NULL DEFAULT 0,
                               error_message     TEXT,
                               CONSTRAINT ck_ingestion_run_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED')),
                               CONSTRAINT ck_ingestion_run_source_not_blank CHECK (length(trim(source)) > 0),
                               CONSTRAINT ck_ingestion_run_counts_non_negative CHECK (
                                   airports_written >= 0 AND flights_written >= 0 AND rejected >= 0
                                   ),
                               CONSTRAINT ck_ingestion_run_finished_after_start CHECK (finished_at IS NULL OR finished_at >= started_at)
);

CREATE INDEX idx_ingestion_run_source_started ON ingestion_run (source, started_at DESC);