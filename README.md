# Flight Booking Backend

A flight ticket booking backend built with **Kotlin + Spring Boot** and **PostgreSQL**. A portfolio project focused on real backend problems — concurrent seat bookings, reservation expiry, search, login, authorization — rather than on infrastructure for its own sake.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.4.20, Java 25 toolchain |
| Framework | Spring Boot 4.1.1 (Web, Data JPA, Validation, Actuator) |
| Database | PostgreSQL 18 |
| Messaging | Kafka (KRaft) for notification events |
| Migrations | Flyway (`ddl-auto: validate`) |
| Security | Spring Security + JWT in an httpOnly cookie, BCrypt, login attempt limiting |
| Tests | Kotlin test + Kotest, Testcontainers, ArchUnit |
| Containers | Podman (`podman kube play`) for PostgreSQL, Mailpit and Kafka |
| Frontend | React + Vite + TypeScript |

## Architecture

A **hexagonal (ports and adapters)** architecture split into Gradle modules; dependencies always point inward:

```
bootstrap  ──▶  adapter:web
           ──▶  adapter:persistence
           ──▶  adapter:security
           ──▶  adapter:provider
           ──▶  adapter:scheduler
           ──▶  adapter:mail (Kafka producer/consumer + SMTP)
           ──▶  adapter:payment
           ──▶  adapter:currency
                    │
                    ▼
              core:application  ──▶  core:domain
```

```
backend/
├── core/
│   ├── domain/            # pure Kotlin: models, invariants, value objects
│   └── application/       # use cases and ports (interfaces), transaction boundaries
├── adapter/               # web, persistence, security, provider, scheduler, mail, payment, currency
└── bootstrap/             # application class, bean wiring, config, error mapping, ITs

frontend/   React + Vite + TypeScript
infra/      Podman pod (PostgreSQL, Mailpit, Kafka)
```

Rules:

- `domain` and `application` contain **no Spring or Jakarta Persistence code**; they compile with `kotlin-stdlib` alone.
- `application` depends only on `domain`; adapters depend on `application` and never on each other.
- The application class lives in `bootstrap` (`dev.maxaure.flightbooking.bootstrap.FlightBookingApplication`).

These rules are enforced by ArchUnit tests.

## Getting started (Podman)

Requires Podman and `envsubst` (package `gettext`).

**1. Configure**

```bash
cd infra
cp .env.example .env
```

Fill in the placeholders in `infra/.env`:

```bash
openssl rand -base64 48   # JWT_SECRET (at least 32 characters)
openssl rand -base64 32   # PASSPORT_ENCRYPTION_KEY (32 bytes, Base64)
```

`COOKIE_SECURE` must be `false` for plain-HTTP localhost.

**2. Start the stack**

```bash
./up.sh
```

`up.sh` builds the backend and frontend images, substitutes `infra/.env` into `infrastructure.yaml` and starts the `flights-infra` pod.

| Service | Address |
|---|---|
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| PostgreSQL | localhost:5432 |
| Kafka | localhost:9092 |
| Mailpit SMTP / UI | localhost:1025 / http://localhost:8025 |

**3. Stop and reset**

```bash
podman kube down infra/infrastructure.yaml          # keeps the pgdata volume
podman kube down infra/infrastructure.yaml && podman volume rm pgdata   # fresh database
```

**Dependencies only**

To run the backend and frontend yourself while Podman provides the infrastructure:

```bash
./infra/up-deps.sh
podman kube down infra/dependencies.yaml
```

`up-deps.sh` starts only PostgreSQL, Kafka and Mailpit — the backend and frontend are not part of that pod. It only needs the `DB_*` and `KAFKA_*` values from `infra/.env`. Never run `up.sh` and `up-deps.sh` at the same time — they share host ports and the `pgdata` volume.

## Configuration

All variables are read by the backend and can be exported or put in `infra/.env`. Required variables have no default. The JDBC URL is built from `DB_HOST` and `DB_PORT` (defaults `localhost` and `5432`) plus `DB_NAME`.

| Variable | Default | Purpose |
|---|---|---|
| `DB_NAME` | `flights` | database name |
| `DB_USER` | `flights` | database user |
| `DB_PASSWORD` | `change-me` | database password |
| `JWT_SECRET` | — | HMAC key for signing access tokens, at least 32 characters |
| `JWT_TTL_MINUTES` | `15` | access token lifetime, also the cookie max-age |
| `REFRESH_TTL_DAYS` | `7` | refresh token lifetime |
| `PASSPORT_ENCRYPTION_KEY` | — | Base64-encoded 32-byte AES key for passport and phone encryption |
| `COOKIE_SECURE` | `true` | set to `false` for plain-HTTP localhost |
| `COOKIE_SAME_SITE` | `Lax` | `SameSite` attribute of the session cookie |
| `SWAGGER_UI_ENABLED` | `false` | expose Swagger UI and `/v3/api-docs` |
| `LOGIN_MAX_FAILURES` | `5` | failed logins allowed per address + email inside the window |
| `LOGIN_ATTEMPT_WINDOW_MINUTES` | `15` | length of the login attempt window |
| `VERIFICATION_CODE_TTL_MINUTES` | `15` | lifetime of a verification code, also the resend backoff ceiling |
| `PENDING_REGISTRATION_PURGE_ENABLED` | `true` | run the scheduled purge of expired pending registrations |
| `PENDING_REGISTRATION_PURGE_CRON` | `0 0 * * * *` | purge schedule |
| `REVOKED_TOKEN_PURGE_ENABLED` | `true` | run the scheduled purge of expired revoked tokens |
| `REVOKED_TOKEN_PURGE_CRON` | `0 0 * * * *` | purge schedule |
| `RESERVATION_EXPIRY_ENABLED` | `true` | run the scheduled reservation expiry sweeper |
| `RESERVATION_EXPIRY_CRON` | `0 * * * * *` | expiry schedule |
| `APP_NAME` | `flight-booking` | product name used in e-mail subjects and bodies |
| `MAIL_ENABLED` | `true` | register the SMTP `MailSender` |
| `MAIL_HOST` | `localhost` | SMTP host (Mailpit) |
| `MAIL_PORT` | `1025` | SMTP port |
| `MAIL_USERNAME` | empty | SMTP username |
| `MAIL_PASSWORD` | empty | SMTP password |
| `MAIL_SMTP_AUTH` | `false` | enable SMTP authentication |
| `MAIL_STARTTLS` | `false` | enable STARTTLS |
| `MAIL_FROM` | `no-reply@flight-booking.local` | `From` address of outgoing mail |
| `PAYMENT_PROVIDER` | `fake` | payment gateway adapter; `fake` uses the deterministic test cards |
| `CURRENCY_DEFAULT` | `PLN` | currency used when a request does not choose one |
| `FILE_PROVIDER_ENABLED` | — | enable the file-based catalogue feed provider |
| `FILE_PROVIDER_LOCATION` | `classpath:feed-ci.json` | classpath or file path of the feed document |
| `PULL_ENABLED` | — | enable periodic feed pulls |
| `PULL_ON_STARTUP` | — | pull the feed once when the application starts |
| `PULL_CRON` | — | feed pull schedule |
| `PULL_HORIZON_DAYS` | — | how far ahead the feed is requested |
| `KAFKA_BOOTSTRAP_SERVERS` | — | Kafka bootstrap servers, for example `localhost:9092` |
| `NOTIFICATION_TOPIC` | — | topic carrying notification events |
| `NOTIFICATION_CONSUMER_GROUP` | — | consumer group of the mail consumer |
| `NOTIFICATION_KAFKA_ENABLED` | — | `false` swaps in a no-op publisher and disables the consumer |
| `PAYMENT_REMINDER_ENABLED` | — | run the payment reminder sweeper |
| `PAYMENT_REMINDER_LEAD_MINUTES` | — | how long before hold expiry the reminder is sent |
| `PAYMENT_REMINDER_CRON` | — | payment reminder schedule |
| `DEPARTURE_REMINDER_ENABLED` | — | run the pre-departure reminder sweeper |
| `DEPARTURE_REMINDER_LEAD_HOURS` | — | how long before departure the reminder is sent |
| `DEPARTURE_REMINDER_CRON` | — | departure reminder schedule |

Actuator exposes only `health` and `info` over HTTP.

**Running the backend alone**

With PostgreSQL, Kafka and Mailpit already running, set these variables before `./gradlew :bootstrap:bootRun` (values are described in the table above):

| Variable |
|---|
| `DB_NAME` |
| `DB_USER` |
| `DB_PASSWORD` |
| `JWT_SECRET` |
| `PASSPORT_ENCRYPTION_KEY` |
| `COOKIE_SECURE` |
| `FILE_PROVIDER_ENABLED` |
| `PULL_ENABLED` |
| `PULL_ON_STARTUP` |
| `PULL_CRON` |
| `PULL_HORIZON_DAYS` |
| `KAFKA_BOOTSTRAP_SERVERS` |
| `NOTIFICATION_TOPIC` |
| `NOTIFICATION_CONSUMER_GROUP` |
| `NOTIFICATION_KAFKA_ENABLED` |
| `PAYMENT_REMINDER_ENABLED` |
| `PAYMENT_REMINDER_LEAD_MINUTES` |
| `PAYMENT_REMINDER_CRON` |
| `DEPARTURE_REMINDER_ENABLED` |
| `DEPARTURE_REMINDER_LEAD_HOURS` |
| `DEPARTURE_REMINDER_CRON` |

## API documentation

The API is described by OpenAPI. From the full stack, the docs are at:

- Swagger UI — http://localhost:8080/swagger-ui.html
- OpenAPI JSON — http://localhost:8080/v3/api-docs

They are disabled by default and return 404 unless `SWAGGER_UI_ENABLED=true` is set.

## Domain

| Table | Holds |
|---|---|
| `airport` | IATA code, name, city, country, time zone |
| `flight` | flight number, route, departure/arrival, status, fare classes |
| `fare_class` | cabin, price, multiplier |
| `flight_seat` | seat number, status (`AVAILABLE` / `HELD` / `SOLD`), hold expiry, optimistic-lock version |
| `reservation` | user, flight, status (`PENDING` / `CONFIRMED` / `EXPIRED` / `CANCELLED`), expiry |
| `reservation_seat` | seat, passenger name, date of birth, encrypted passport and phone, last four digits |
| `payment` | reservation, idempotency key, issuer result, status |
| `users` | email, BCrypt hash, profile, `email_verified_at` |
| `pending_registration` | unverified registration with code hash, expiry and attempt counter |
| `revoked_token` | logged-out JWT ids until their expiry |
| `ingestion_run` | audit of each feed pull |

Seats are generated per flight (30 rows × A–F). The schema is owned exclusively by Flyway migrations and Hibernate runs with `ddl-auto: validate`, so an entity/migration mismatch fails at startup.

A booking is a short hold, not a sale: a user searches flights, holds one or more seats, and pays before `expires_at`. Payment confirms the reservation and marks the seats `SOLD`; an expired hold is swept by a scheduler, which releases the seats. Concurrent holds are resolved by optimistic locking on `flight_seat.version`.

## Authentication

Accounts are created only by verifying an e-mail; registration never writes to `users`.

- `POST /api/auth/register` stores a pending registration (BCrypt password and code hashes, expiry, attempt counter) and mails a 6-digit code, answering `202`. If the address already has an account, the response is identical and no mail is sent, so the endpoint cannot reveal accounts.
- `POST /api/auth/verify` claims one attempt, checks the code, creates the user and starts a session. Every failure — unknown address, wrong code, expired row, exhausted attempts — answers the same `400 invalid_or_expired_code`.
- A session is a signed JWT in an httpOnly cookie named `access_token` (15 minutes) plus an opaque `refresh_token` cookie scoped to `/api/auth` (7 days). The `AuthFilter` validates the access token on every request; controllers never read a user id from the request and use `@CurrentUser` instead.
- `POST /api/auth/refresh` rotates the refresh token: the presented one is revoked and a new one is issued in the same family. Reusing an already rotated token revokes the whole family.
- Logout revokes both the JWT id (`revoked_token`) and the refresh token; a scheduled job deletes expired rows.
- Failed logins are limited per address + email (`429` with `Retry-After`); verification resends back off exponentially (60 s, 5 min, 15 min).
- Passport and phone numbers are encrypted with AES-256-GCM using `PASSPORT_ENCRYPTION_KEY`; only the last four digits stay in plaintext, and the API returns `••••1234`.

A cookie instead of an `Authorization` header keeps tokens out of `localStorage`, so injected JavaScript cannot read them, and a stateless access token allows horizontal scaling without sticky sessions. `SameSite=Lax` keeps the cookie off cross-site requests.

## Notifications

Use cases publish a `NotificationEvent` through the `NotificationPublisher` port; `adapter:mail` implements it with a Kafka producer, and a consumer in the same adapter renders a template and hands it to the SMTP `MailSender` (Mailpit locally). Each event carries the recipient address, so the consumer needs no database lookup.

Notifications are sent for: a confirmed reservation, a payment reminder before the hold expires, an expired reservation, a delayed or cancelled flight, and a pre-departure reminder. Reminder sweeps mark reservations after publishing, so each one is reminded once.

With `NOTIFICATION_KAFKA_ENABLED=false` the publisher is a no-op and no consumer is created, which is how the integration tests run without a broker.

## Payments

Payments use a fake gateway (`PAYMENT_PROVIDER=fake`, the default) instead of a real PSP, so the portfolio project stays self-contained. `POST /api/v1/reservations/{id}/payment` takes `idempotencyKey`, `cardNumber` and `cardHolder`; approval confirms the reservation and marks the seats `SOLD`, a decline answers `402`, and replaying the key returns the stored payment instead of charging again. The gateway sits behind a port, so the fake can be replaced by a real provider adapter without touching the use cases.

## Currency

Prices are converted server-side when a request carries `?currency=XXX`, using static demo rates from `app.currency`; the `CurrencyConverter` port means a real FX provider can replace those rates without touching the use cases.

## Catalogue feed

Flights come from a file-based provider (`FILE_PROVIDER_ENABLED`, location `FILE_PROVIDER_LOCATION`) — a stand-in for a real airline feed, so the project runs without external services. The default location is `classpath:feed-ci.json`, which ships with the backend, so the application starts without any extra file. Like the payment gateway, it is a full adapter behind a port and can be swapped for a real feed with no change to the application layer.

## Testing

```bash
cd backend
./gradlew test
```

The suite uses Testcontainers, so it needs a Docker-compatible runtime (Docker, or Podman with `DOCKER_HOST` pointing at its socket). It covers registration and verification, login and session handling, reservations and seat ownership, and payments including idempotent replays; the catalogue is seeded per test through SQL, so the tests never depend on feed ingestion.

The frontend tests run with:

```bash
cd frontend
npm test
```

CI (`.github/workflows/ci.yml`) runs the backend tests, the frontend typecheck, build and tests, and starts the whole Podman stack to run the load tests.

Load tests live in `load/` and use k6, so they need a running stack and `k6` on the PATH. The scenarios are `smoke.js` (health and search), `booking.js` (login, hold, pay) and `contention.js` (50 clients racing for one seat — exactly one hold may win). Locally:

```bash
./load/run.sh smoke.js
./load/run.sh booking.js
./load/run.sh contention.js
./load/run.sh cleanup
```

The script creates and verifies a load-test account through Mailpit, and releases its reservations afterwards; `KEEP_DATA=1` skips the cleanup and `LOAD_EMAIL`/`LOAD_PASSWORD` override the account. In CI the stack is thrown away instead.

## License

[MIT](LICENSE)


