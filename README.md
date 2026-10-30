# Flight Booking Backend

A flight ticket booking backend built with **Kotlin + Spring Boot** and **PostgreSQL**, created as a portfolio project. The focus is on solving real backend problems (concurrency, reservation expiry, search, security) rather than on infrastructure for its own sake.

> Status: early development. See the [Roadmap](#roadmap) and [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).

## Goals

- Clean, well-tested backend code
- Correct handling of concurrent seat bookings
- Reservations that hold a seat for a limited time and expire if not paid
- Searchable flights with filtering and pagination
- A reproducible local setup using Podman only (no Docker required)
- Local-first, but able to deploy to a free-tier platform on demand

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.4.20, Java 25 toolchain |
| Framework | Spring Boot 4.1.1 (Web, Data JPA, Validation, Actuator) |
| Database | PostgreSQL 18 |
| Messaging | Kafka (KRaft) for notification events |
| Migrations | Flyway (schema is owned by migrations, `ddl-auto: validate`) |
| Security | Spring Security + JWT in an httpOnly cookie, BCrypt, login attempt limiting |
| API docs | springdoc-openapi (planned) |
| Tests | Kotlin test + Kotest, Testcontainers for integration tests, ArchUnit for architecture rules (planned) |
| Containers | Podman (`podman kube play`) for PostgreSQL, Mailpit and Kafka |
| Frontend | React + Vite + TypeScript |

## Architecture

A **hexagonal (ports and adapters) architecture** split into Gradle modules. Dependencies always point inward:

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
├── adapter/
│   ├── web/               # REST controllers, DTOs, auth filter, @CurrentUser
│   ├── persistence/       # JPA entities, Spring Data repositories, mappers, Flyway migrations
│   ├── security/          # BCrypt, JWT issuer, login attempt limiter
│   ├── provider/          # feed providers (file-based catalogue feed)
│   └── scheduler/         # reservation expiry, feed pull, revoked-token purge
└── bootstrap/             # application class, bean wiring, security config, error mapping, ITs
```

Rules:

- `domain` and `application` contain **no Spring or Jakarta Persistence code**; they must compile with `kotlin-stdlib` alone.
- `application` depends only on `domain`.
- Adapters depend on `application` and must not depend on each other.
- The Spring Boot application class lives in `bootstrap` (`dev.maxaure.flightbooking.bootstrap.FlightBookingApplication`).

These rules are enforced by ArchUnit tests (planned).

## Data model (initial)

- **airport**: `code` (e.g. WAW), `name`, `city`, `country`
- **flight**: `flight_number`, `origin`, `destination`, `departure_at`, `arrival_at`, `price`, `status`
- **flight_seat**: `flight`, `seat_number` (e.g. 12A), `status` (AVAILABLE / HELD / SOLD), `version` (optimistic locking)
- **users**: `email` (unique, lowercase), `password_hash`, `created_at`, `email_verified_at`
- **pending_registration**: `email` (primary key), `password_hash`, `code_hash`, `expires_at`, `attempts`, `created_at`, `sent_at`, `send_count`
- **revoked_token**: `token_id`, `expires_at`
- **reservation**: `user`, `flight`, `status` (PENDING / CONFIRMED / EXPIRED / CANCELLED), `expires_at`, `created_at`
- **reservation_seat**: `reservation`, `flight_seat`, `passenger_name`

Seats are generated when a flight is created (for example 30 rows × A–F). Aircraft types and cabin layouts are intentionally out of scope for now.

The schema is managed **exclusively** by Flyway migrations in `backend/adapter/persistence/src/main/resources/db/migration`. Hibernate runs with `ddl-auto: validate`, so a mismatch between entities and migrations fails the application at startup.

## Repository layout

```
flight-booking/
├── backend/              # Gradle multi-module Kotlin backend
├── frontend/             # React + Vite + TypeScript
├── docs/                 # decisions, plans, open work
└── infra/
    └── infrastructure.yaml   # Podman pod: PostgreSQL, Mailpit and Kafka
```

## Running the whole stack (Podman)

Requires Podman and `envsubst` (package `gettext`).

```bash
cd infra
cp .env.example .env   # first time only, replace the <placeholders>
./up.sh
```

`up.sh` builds the backend and frontend images, substitutes `infra/.env` into `infrastructure.yaml` and starts the `flights-infra` pod:

| Service | Address |
|---|---|
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| PostgreSQL | localhost:5432 |
| Kafka | localhost:9092 |
| Mailpit SMTP / UI | localhost:1025 / http://localhost:8025 |

Stop it (the `pgdata` volume is kept):

```bash
podman kube down infra/infrastructure.yaml
```

Reset the database between runs:

```bash
podman kube down infra/infrastructure.yaml
podman volume rm pgdata
```

## Running only the dependencies (IDE)

To run the backend and frontend from the IDE while Podman provides the services:

```bash
./infra/up-deps.sh
```

This starts only PostgreSQL, Kafka and Mailpit in the `flights-deps` pod. Configure the backend with the variables from `backend/local-env` and start the frontend with `npm run dev`. Stop with:

```bash
podman kube down infra/dependencies.yaml
```

Do not run `up.sh` and `up-deps.sh` at the same time — they use the same host ports and the same `pgdata` volume.

## Running locally

Only the dependencies run in containers when you develop against an IDE. The app itself runs directly on the host.

**1. Start the infrastructure**

```bash
cd infra
cp .env.example .env   # first time only
./up-deps.sh
```

`up-deps.sh` loads `infra/.env` and substitutes the `POSTGRES_*` and `KAFKA_*` values into the pod. The pod publishes PostgreSQL 18 on `localhost:5432`, Mailpit (SMTP `1025`, UI `8025`) and Kafka on `localhost:9092`.

To stop it (the `pgdata` volume is kept):

```bash
podman kube down infra/dependencies.yaml
```

On macOS/Windows, first run `podman machine init` and `podman machine start`.

**2. Run the backend**

```bash
cd backend
./gradlew :bootstrap:bootRun
```

**3. Configuration**

Required variables have no default and must be set before starting the app.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | — | JDBC URL of the PostgreSQL instance |
| `DB_USER` | — | database user |
| `DB_PASSWORD` | — | database password |
| `JWT_SECRET` | — | HMAC key for signing access tokens, at least 32 characters |
| `JWT_TTL_MINUTES` | `60` | access token lifetime, also the cookie max-age |
| `PASSPORT_ENCRYPTION_KEY` | — | Base64-encoded 32-byte AES key for passport and phone encryption |
| `COOKIE_SECURE` | `true` | set to `false` for plain-HTTP local development |
| `COOKIE_SAME_SITE` | `Lax` | `SameSite` attribute of the session cookie (use `Strict` for same-site frontends) |
| `LOGIN_MAX_FAILURES` | `5` | failed logins allowed per address + email inside the window |
| `LOGIN_ATTEMPT_WINDOW_MINUTES` | `15` | length of the login attempt window |
| `VERIFICATION_CODE_TTL_MINUTES` | `15` | lifetime of a verification code, also the resend backoff ceiling |
| `PENDING_REGISTRATION_PURGE_ENABLED` | `true` | run the scheduled purge of expired pending registrations |
| `PENDING_REGISTRATION_PURGE_CRON` | `0 0 * * * *` | purge schedule |
| `APP_NAME` | `flight-booking` | product name used in the verification e-mail subject and body |
| `MAIL_ENABLED` | `true` | register the SMTP `MailSender`; set `false` for tests that do not need mail |
| `MAIL_HOST` | `localhost` | SMTP host (Mailpit in development) |
| `MAIL_PORT` | `1025` | SMTP port |
| `MAIL_USERNAME` | — | SMTP username, empty for Mailpit |
| `MAIL_PASSWORD` | — | SMTP password, empty for Mailpit |
| `MAIL_SMTP_AUTH` | `false` | enable SMTP authentication |
| `MAIL_STARTTLS` | `false` | enable STARTTLS |
| `MAIL_FROM` | `no-reply@flight-booking.local` | `From` address of outgoing mail |
| `PAYMENT_PROVIDER` | `fake` | payment gateway adapter; `fake` uses the deterministic test cards |
| `CURRENCY_DEFAULT` | `PLN` | currency used when a request does not choose one |
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
| `REVOKED_TOKEN_PURGE_ENABLED` | `true` | run the scheduled purge of expired revoked tokens |
| `REVOKED_TOKEN_PURGE_CRON` | `0 0 * * * *` | purge schedule |
| `RESERVATION_EXPIRY_ENABLED` | `true` | run the scheduled reservation expiry sweeper |
| `RESERVATION_EXPIRY_CRON` | `0 * * * * *` | expiry schedule |
| `FILE_PROVIDER_ENABLED` | — | enable the file-based catalogue feed provider |
| `FILE_PROVIDER_LOCATION` | — | classpath or file path of the feed document |
| `PULL_ENABLED` | — | enable periodic feed pulls |
| `PULL_ON_STARTUP` | — | pull the feed once when the application starts |
| `PULL_CRON` | — | feed pull schedule |
| `PULL_HORIZON_DAYS` | — | how far ahead the feed is requested |

Actuator exposes only `health` and `info` over HTTP.

**Infrastructure variables (`infra/.env`)**

`up.sh` substitutes these into `infrastructure.yaml`.

| Variable | Default in `.env.example` | Purpose |
|---|---|---|
| `POSTGRES_DB` | `flights` | database name |
| `POSTGRES_USER` | `flights` | database user |
| `POSTGRES_PASSWORD` | `change-me` | database password |
| `KAFKA_NODE_ID` | `1` | KRaft node id |
| `KAFKA_PROCESS_ROLES` | `broker,controller` | single-node KRaft roles |
| `KAFKA_LISTENERS` | `PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093` | broker and controller listeners |
| `KAFKA_ADVERTISED_LISTENERS` | `PLAINTEXT://localhost:9092` | address clients receive |
| `KAFKA_CONTROLLER_LISTENER_NAMES` | `CONTROLLER` | controller listener name |
| `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP` | `CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT` | listener protocol mapping |
| `KAFKA_CONTROLLER_QUORUM_VOTERS` | `1@localhost:9093` | KRaft quorum |
| `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` | `1` | single-broker replication |
| `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR` | `1` | single-broker replication |
| `KAFKA_TRANSACTION_STATE_LOG_MIN_ISR` | `1` | single-broker ISR |
| `KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS` | `0` | faster consumer startup in development |

## Authentication

Accounts are stored in `users` (email lowercased, BCrypt hash, `created_at`). Login issues a signed JWT in an httpOnly cookie named `access_token`; the frontend sends it automatically with `credentials: 'include'`.

- Authentication state is stateless: the `AuthFilter` validates the cookie on every request and puts the `UserId` into the security context.
- Controllers never read a user id from the request. `@CurrentUser UserId` resolves it from the security context.
- Logout writes the token id into `revoked_token`; a scheduled job deletes rows once they can no longer be presented.
- Failed logins are counted per client address and email. After `LOGIN_MAX_FAILURES` failures inside the window, further attempts get `429` with `Retry-After`, and a successful login clears the counter.
- The client address comes from `HttpServletRequest.remoteAddr`. Behind a reverse proxy set `server.forward-headers-strategy` so the real client address is used.

### E-mail verification

Accounts are created only by a verified code. Registration never writes to `users`.

- `POST /api/auth/register` upserts `pending_registration` (BCrypt password hash, BCrypt code hash, expiry, attempts) and mails a 6-digit code, answering `202 {"stage":"CODE_SENT"}`. Repeating it replaces the row and issues a new code. If the address already has an account the answer is the same `202` with `{"stage":"EMAIL_VERIFIED"}` and **no mail is sent**, so the endpoint cannot be used to discover accounts.
- `POST /api/auth/verify` claims one attempt atomically, checks the code and creates the verified user in `users`, deleting the pending row. Between the claim and the insert the attempt is already committed, so a crash still spends it. It issues the session cookie.
- `/verify` answers one generic `400 invalid_or_expired_code` for an unknown address, a wrong code, an expired row and an exhausted row; every failure path also pays for a BCrypt comparison.
- At five attempts the row is locked and a new code is needed; `LoginAttemptLimiter` (per address + email) sits on top and answers `429` with `Retry-After`.
- `/verify/resend` is limited per e-mail and `remoteAddr`, counts every request and never swallows its `429`. An unknown address, an existing account, an expired row or a request inside the cooldown all get a silent `204`.
- Resends back off exponentially: 60 s after the first mail, then 5 min, then 15 min, capped at the code TTL.
- Expired rows are deleted by the scheduled purge and replaced by the next `register`.
- `POST /api/auth/login` only looks at `users`, so an unfinished registration cannot sign in. An expired/unverified session never reaches a reservation; holding a seat still answers `403` for a user without `email_verified_at`.
- Mail goes out through the `adapter:mail` module (`MailSender` port, `SmtpMailSender` adapter) using the `verification` template from `classpath:/mail` (subject, plain text and HTML alternatives) with `{appName}`, `{code}` and `{ttlMinutes}` placeholders. Mailpit is part of `infra/infrastructure.yaml` (SMTP on `1025`, UI on `8025`).

### Why a JWT cookie instead of a session

The backend must stay stateless so it can be scaled horizontally and deployed to a free tier without sticky sessions. A cookie rather than an `Authorization` header keeps the token out of `localStorage`, so injected JavaScript cannot read it. Since a stateless token cannot be un-issued, logout relies on the `revoked_token` table.

## Frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server proxies `/api` to the backend, so both run on the same origin and no CORS configuration is needed. If you serve the frontend from a different origin, add an explicit CORS bean with `allowCredentials(true)` and a fixed list of origins.

## Deployment path

Local-first, cloud-ready:

1. PostgreSQL runs in a Podman pod from `infra/infrastructure.yaml`; the backend and frontend run on the host.
2. The backend is a single Spring Boot jar (`./gradlew :bootstrap:bootJar`) configured through environment variables only.
3. Flyway migrations run at startup, so a new environment only needs an empty database plus `DB_URL`, `DB_USER`, `DB_PASSWORD` and `JWT_SECRET`.
4. The frontend builds to static files (`npm run build`) and can be served by any static host or a CDN, pointing `/api` at the backend.
5. The backend and frontend also run as containers in the same Podman pod (`infra/up.sh`), and a CI job starts the whole stack.

## Fake payments

`PAYMENT_PROVIDER=fake` (the default) swaps in a deterministic gateway, so no real PSP is called:

- `4242 4242 4242 4242` — approved
- `4000 0000 0000 0002` — declined by the issuer
- `4000 0000 0000 9995` — declined for insufficient funds

`POST /api/v1/reservations/{id}/payment` takes `idempotencyKey`, `cardNumber` and `cardHolder`, and confirms the reservation on approval. Replaying a key returns the stored payment instead of charging again; paying a confirmed or expired hold answers `409`.

## Security notes

Passport numbers, phone numbers and dates of birth are required for every passenger on a reservation seat. `PassportCipher` encrypts passport and phone numbers with AES-256-GCM using a random 12-byte IV per value and stores `v1:<iv>:<ciphertext>` in `reservation_seat.passport_number_ciphertext` and `reservation_seat.phone_number_ciphertext`. Only the last four digits are kept in plaintext (`passport_number_last4`, `phone_number_last4`), the API returns `••••1234`, and reads never decrypt. Dates of birth are stored in `reservation_seat.date_of_birth` and returned as ISO dates. Phone numbers must start with `+` followed by 7–15 digits, for example `+48600123456`. The key comes from `PASSPORT_ENCRYPTION_KEY` (Base64-encoded 32 bytes) and must never be committed; see `infra/.env.example`. The `v1:` prefix leaves room for key rotation.

This is a local, single-key setup. Generate the key with `export PASSPORT_ENCRYPTION_KEY=$(openssl rand -base64 32)` before starting the backend. A production deployment would use KMS/HSM-backed envelope encryption, per-tenant keys, a retention/purge policy, audit logging on decrypt, and a separate service that owns sensitive documents. Passport and phone values must never be logged, put in exception messages, or included in snapshots.

## Currency

`GET /api/v1/currencies` returns the default and supported codes. Prices are converted server-side when a request carries `?currency=XXX` (`/flights`, `/flights/deals`, `/flights/{id}/seats` and the reservation endpoints), and payments are charged in the selected currency.

Rates live in `app.currency` (`base`, `default`, `rates`) and are static demo values — no FX provider is called. `CurrencyConverter` is the port; to plug a real FX API, add one adapter bean and leave the use cases untouched. The header selector persists the choice in `localStorage`.

## Testing with Podman

Testcontainers expects a Docker-compatible socket. With Podman:

```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

Integration tests start their own PostgreSQL container. They live in `backend/bootstrap/src/test` and use Kotest with `SpringExtension`:

- `AuthTest` — register, duplicate email, malformed email, login, wrong password, unknown email, email case, short password, tampered cookie, logout, token expiry
- `ReservationTest` — a user only sees their own reservations, unknown user gets an empty list, cross-user cancel returns `404`
- `PaymentTest` — paying a hold with the fake gateway: approval and sold seats, decline, idempotent replay, key reuse across reservations, cross-user payment, expired hold

The catalogue is seeded per test by `CatalogueTestSeed`, which inserts airports, one flight, a fare class and a seat inventory directly through SQL. The tests never depend on feed ingestion.

Unit tests run without containers:

```bash
cd backend
./gradlew test -x :bootstrap:test
```

## Roadmap

- [x] Project plan and data model
- [x] Gradle multi-module skeleton (hexagonal) + PostgreSQL 18 in Podman
- [ ] Enforce module boundaries with ArchUnit
- [x] `V1__init.sql` (airports, flights, seats) and persistence adapters
- [x] Catalogue ingestion from a feed provider, automatic seat generation
- [x] Flight search with filters and pagination (`GET /api/v1/flights?origin=WAW&destination=LIS&date=...`)
- [x] Global error handling (RFC 9457 `ProblemDetail`)
- [x] Users, Spring Security, JWT in an httpOnly cookie
- [x] Login rate limiting and token revocation
- [x] Reservations: seat holding, optimistic locking, expiry scheduler
- [x] Protected reservation endpoints (`POST /api/v1/reservations/flights/{id}`, `DELETE /api/v1/reservations/{id}`)
- [x] `GET /api/v1/reservations` — the caller's own reservations
- [x] Frontend auth flow and "My reservations" page (React + Vite + TypeScript)
- [x] E-mail verification on registration, password reset, optional TOTP two-factor
- [ ] Pricing engine (fare classes, taxes, price snapshot on reservation)
- [x] Payments behind a fake gateway, with idempotency keys
- [x] Events: Kafka → e-mail (Mailpit)
- [ ] Transactional outbox before Kafka, so a broker outage cannot lose an event
- [ ] Check-in and signed boarding passes
- [x] Tests with Testcontainers (written alongside features)
- [ ] OpenAPI docs
- [x] Frontend (React + Vite + TypeScript)

Detailed staging, acceptance criteria and open questions: [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).

### Later / optional

- [ ] Containerize the backend (`./gradlew bootBuildImage` or a `Containerfile`) and add it to `infrastructure.yaml`
- [x] Add Kafka (KRaft) and Mailpit to the Podman pod
- [ ] Move credentials to Kubernetes `Secret` and configuration to `ConfigMap`
- [ ] Notifications, next steps:
  1. Outbox pattern between the use case and Kafka
  2. Separate `notification-service` consuming events from Kafka
- [ ] Free-tier deployment (local-first, cloud-ready)
- [ ] Kubernetes deployment

### Account verification and stronger login (planned)

Registration and login today rely on a password alone. The next iteration adds proof that the user controls the mailbox, and reduces what a stolen password is worth.

**1. E-mail verification on registration**

- New `users` columns: `email_verified_at` (nullable) and `verification_code_hash`, migrated in `V9`.
- `POST /api/auth/register` creates the account unverified, stores a BCrypt hash of a 6-digit code, and sends the code by e-mail.
- `POST /api/auth/verify` takes the code, marks the account verified and issues the session cookie.
- Login stays possible but unverified accounts only get read access; anything that creates a reservation requires a verified address.
- Code rules: 15-minute TTL, single use, at most 5 attempts per code, a new code invalidates the previous one.
- Resend endpoint `POST /api/auth/verify/resend`, limited by the same `LoginAttemptLimiter` keyed per address + email.

**2. Password reset**

- `POST /api/auth/password/forgot` always answers `204`, whether or not the address exists, so it cannot be used to enumerate accounts.
- The e-mail carries a single-use token whose hash is stored with an expiry, instead of a code, so it survives being opened on another device.
- `POST /api/auth/password/reset` consumes the token and invalidates every outstanding session of that user.

**3. Two-factor authentication (TOTP)**

- Optional per user: an authenticator secret stored encrypted at rest, plus ten single-use recovery codes.
- Login with 2FA enabled answers `202` and a short-lived challenge token, exchanged for a session through `POST /api/auth/2fa`.
- Attempts are counted by the existing limiter, keyed per address + email + challenge.

**4. Account deletion contract**

`reservation.user_id` is `NOT NULL` with `ON DELETE RESTRICT`, so an account cannot simply be removed. Deletion must either cancel every `PENDING`/`CONFIRMED` reservation first or be refused while holds are active. This has to be decided before an endpoint exists.

**Infrastructure these need**

- An outbound e-mail port (`MailSender` in the application layer) with an SMTP adapter; Mailpit in development, a real relay in production.
- A queue or retry policy for delivery failures, so a provider outage does not lose a verification e-mail.
- Mailbox abuse limits: verification and reset mails are throttled per address, independently of the login limiter.

**Open questions**

- Whether unverified accounts expire and get cleaned up, or stay dormant indefinitely. -- for now do not worry about it
- Whether verification is mandatory before a reservation, or only before paying for one. -- is mandatory for reservation
- Whether codes are sent as plain text only, or with an HTML alternative. --> simple HTML 
- Registration for an address that already has an account answers the same `202` and mails nothing, so it no longer reveals anything. The trade-off is that a user who forgot they signed up receives no reminder; a "this address already has an account" mail would fix that but needs the same asynchronous path as password reset.

** NOTES:** 
- Verification code entropy: a 6-digit code has only a million possibilities, so the 5-attempt limit and 15-minute TTL carry the real security. Make sure attempts are counted per code, not just per IP.
- Reset token: generate it with a SecureRandom of at least 32 bytes, and store only a SHA-256 hash (BCrypt isn’t needed for high-entropy tokens).
- Timing: forgot should take about the same time whether or not the address exists, otherwise response time leaks account existence even with the 204. Doing the email work asynchronously via the outbox helps here naturally.
- 2FA challenge token: bind it to the user and give it a short expiry (a few minutes), single use.
## Notifications

Notifications go to the user who made the reservation. Each event carries the e-mail address, so the consumer needs no database lookup.

Triggers:

- Reservation confirmed after payment
- Payment reminder before the hold expires
- Reservation expired and seat released
- Flight delayed or cancelled (every passenger of the flight)
- Pre-departure reminder

Flow:

- Use cases publish a `NotificationEvent` through the `NotificationPublisher` port.
- `adapter:mail` implements the port with a Kafka producer. Events are JSON messages on `NOTIFICATION_TOPIC`; the consumer in the same adapter renders a template and hands it to `MailSender` (SMTP, Mailpit in development).
- Payments publish only after the reservation is confirmed, and idempotent replays do not publish again.
- Expiry publishes from the existing sweeper, after the seats are released.
- Ingestion publishes a disruption only when a flight transitions from another status to `DELAYED` or `CANCELLED`, so reingesting the same status does not repeat the mail.
- Payment and departure reminders are scheduled sweeps. The selected reservations are marked with `payment_reminder_sent_at` / `departure_reminder_sent_at` after publishing, so a reservation is reminded once; a failed publish leaves it unmarked for the next sweep.
- With `NOTIFICATION_KAFKA_ENABLED=false` the producer is a no-op and the consumer is not created; the integration tests use this so no broker is needed.

Templates live in `adapter/mail/src/main/resources/mail` (`reservation-confirmed`, `payment-reminder`, `reservation-expired`, `flight-disrupted`, `departure-reminder`), each with subject, plain text and HTML alternatives.

This is direct Kafka publishing; broker send failures are logged and the transactional-outbox step in the roadmap closes the gap between a committed reservation and a lost event.
