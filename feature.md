# Flight Booking: Catalogue Ingestion

Kotlin + Spring Boot flight booking system. This document describes how catalogue data (airports and flights) gets into the system, what is already built, and what is left.

## Why flights are stored locally

The system owns seat inventory (`flight_seat` has `HELD`, `SOLD`, `hold_expires_at`, `reservation_id`), so schedules must live in our database. The pull job is a **schedule sync** from a partner or airline. Live price/availability search (aggregator style, like eSky) would be a separate port that never writes to the catalogue.

## Architecture

```
core/
  domain/         Airport, Flight, FlightIdentity, FareClass, ...
  application/    CatalogueIngestionUseCase, repository ports
adapter/
  web/            FeedController (push channel)
  persistence/    JPA entities, mappers, repository implementations
  provider/       FlightDataProvider implementations (outbound)
  scheduler/      FeedPullJob, FeedPullTrigger (inbound timer)
bootstrap/        Spring Boot main, application.yml
```

Rule: adapters depend only on `application` and `domain`, never on each other.

```
Pull:  Scheduler -> FlightDataProvider (file | future vendors) --\
                                                                  +--> CatalogueIngestionUseCase --> DB
Push:  POST /api/v1/admin/feed ----------------------------------/
```

## Done

- [x] `FlightDataProvider` port and `FeedRequest` (application layer)
- [x] `FileFlightDataProvider` reading `feed-large.json` (own DTOs and mapper, no cross-adapter imports)
- [x] Explicit `@Bean` wiring (`ProviderConfig`, `SchedulingConfig`, `UseCaseConfig`)
- [x] Scheduler on virtual threads, startup pull runs asynchronously, cron configurable
- [x] Secrets and settings from environment variables (no defaults in `application.yml`)
- [x] Idempotent upsert: airports by IATA code, flights by `FlightIdentity(flightNumber, departureAt)`, fare ids preserved
- [x] Provenance: `source` and `synced_at` on `airport` and `flight` (Flyway V2), carried through entities and mappers
- [x] Unit tests updated for the new use case signature
- [x] Flyway V3 `ingestion_run` table created (confirm it applied on startup)

## Remaining (original plan)

1. ~~Provider port, provider and scheduler~~ **done**
2. Idempotent ingestion and `ingestion_run`
    - [x] Idempotent upsert, provenance (`source`, `synced_at`)
    - [x] Flyway V3 `ingestion_run` table (confirm it applied)
    - [x] Domain `IngestionRun`, `IngestionStatus`, port `IngestionRunRepository`, JPA adapter
    - [x] Use case records a run; bad records are rejected and counted, status `PARTIAL`
    - [x] Per-record transactions so one failure does not roll back the feed
    - [x] `IngestionSummary` with `runId`, `status`, `rejected`
    - [x] `FeedPullJob` records `FAILED` runs when a provider throws
3. Security of `/api/v1/admin/feed`
    - [x] Keycloak in docker-compose, OAuth2 resource server (JWT)
    - [x] Scope `feed:ingest`, issuer and audience validation, deny-by-default
    - [x] Replace basic auth admin user; use client id as `source`
    - [x] Security tests (401, 403, 200)
4. Validation, limits, audit
    - [ ] Bean Validation on the feed DTO, body size limits, strict Jackson
    - [ ] Audit log (who ingested what); actuator limited to `health`
5. Second adapter to prove the abstraction
    - Already covered by `FileFlightDataProvider` (pull) plus the push endpoint. Nothing more needed unless you want it.
6. Optional extras
    - [ ] ShedLock (only if running more than one instance)
    - [ ] Resilience4j, HMAC signing: skip unless you want them

### Cleanup
- [ ] Remove debug `println` calls (`TRIGGER CREATED`, `TRIGGER onReady`, `JOB onStartup`, `PULL started`, `HERRRR...`, `HEEEE...`)
- [ ] Set cron back to `0 0 */6 * * *`
- [ ] Remove leftover `fake` names and unused `@Value` defaults in config classes
- [ ] Integration test with Testcontainers covering ingest and `source` / `synced_at`
- [ ] Flights outside `horizon-days` keep `source = manual`; decide if that is acceptable

## Configuration

Required environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `OIDC_ISSUER_URI`, `OIDC_AUDIENCE`.

Optional (overridable): `FILE_PROVIDER_ENABLED`, `FILE_PROVIDER_LOCATION`, `PULL_ENABLED`, `PULL_ON_STARTUP`, `PULL_CRON`, `PULL_HORIZON_DAYS`.

Infra (`infra/up.sh`, values from `infra/.env`): `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `KC_BOOTSTRAP_ADMIN_PASSWORD`, `FEED_CLIENT_SECRET`.

Calling the feed requires a Keycloak client-credentials token with scope `feed:ingest` and audience `flight-booking-api`.

## Verifying a pull

```sql
select source, count(*), max(synced_at) from flight  group by source;
select source, count(*), max(synced_at) from airport group by source;
```

Expect `file` with a fresh `synced_at` for flights inside the pull window, and `manual` for older rows.