# Flight Booking Backend

A flight ticket booking backend built with **Kotlin + Spring Boot** and **PostgreSQL**, created as a portfolio project. The focus is on solving real backend problems (concurrency, reservation expiry, search, security) rather than on infrastructure for its own sake.

> Status: early development. See the [Roadmap](#roadmap) and [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

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
| Migrations | Flyway (schema is owned by migrations, `ddl-auto: validate`) |
| Security | Spring Security + JWT (planned) |
| API docs | springdoc-openapi (planned) |
| Tests | JUnit 5 + Testcontainers, ArchUnit for architecture rules (planned) |
| Containers | Podman (`podman kube play`) |
| Frontend | React + Vite + TypeScript (planned) |

## Architecture

A **hexagonal (ports and adapters) architecture** split into Gradle modules. Dependencies always point inward:

```
bootstrap  ──▶  adapter:web
           ──▶  adapter:persistence
           ──▶  adapter:kafka (planned)
                    │
                    ▼
              application  ──▶  domain
```

```
backend/
├── domain/                # pure Kotlin: models, invariants, value objects, domain events
├── application/           # use cases and ports (interfaces), transaction boundaries
├── adapter/
│   ├── web/               # REST controllers, DTOs, error handling, security config
│   └── persistence/       # JPA entities, Spring Data repositories, mappers
└── bootstrap/             # Main class, configuration, wiring, integration-test setup
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
- **flight_seat**: `flight`, `seat_number` (e.g. 12A), `status` (AVAILABLE / HELD / BOOKED), `version` (optimistic locking)
- **app_user**: `email`, `password_hash`, `full_name`
- **reservation**: `user`, `flight`, `status` (PENDING / CONFIRMED / EXPIRED / CANCELLED), `expires_at`, `created_at`
- **reservation_seat**: `reservation`, `flight_seat`, `passenger_name`

Seats are generated when a flight is created (for example 30 rows × A–F). Aircraft types and cabin layouts are intentionally out of scope for now.

The schema is managed **exclusively** by Flyway migrations in `bootstrap/src/main/resources/db/migration`. Hibernate runs with `ddl-auto: validate`, so a mismatch between entities and migrations fails the application at startup.

## Repository layout

```
flight-booking/
├── backend/              # Gradle multi-module Kotlin backend
├── infra/
│   └── infrastructure.yaml   # Podman pod: PostgreSQL (Kafka and Mailpit planned)
├── IMPLEMENTATION_PLAN.md
└── README.md
```

## Running locally

Only infrastructure runs in a container during development. The app itself runs directly on the host.

**1. Start the infrastructure**

```bash
podman kube play infra/infrastructure.yaml
podman pod ps
```

This pod publishes PostgreSQL 18 on `localhost:5432` with database, user and password all set to `flights`.

To stop it (the `pgdata` volume is kept):

```bash
podman kube down infra/infrastructure.yaml
```

On macOS/Windows, first run `podman machine init` and `podman machine start`.

**2. Run the backend**

```bash
cd backend
./gradlew :bootstrap:bootRun
```

**3. Configuration**

The application reads the following environment variables (defaults in brackets):

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/flights` |
| `DB_USER` | `flights` |
| `DB_PASSWORD` | `flights` |

Actuator exposes only `health` and `info` over HTTP.

## Testing with Podman

Testcontainers expects a Docker-compatible socket. With Podman:

```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

Integration tests are planned; the configuration above is what they will use.

## Roadmap

- [x] Project plan and data model
- [x] Gradle multi-module skeleton (hexagonal) + PostgreSQL 18 in Podman
- [ ] Enforce module boundaries with ArchUnit
- [ ] `V1__init.sql` (airports, flights, seats) and persistence adapters
- [ ] CRUD for airports and flights, automatic seat generation
- [ ] Flight search with filters and pagination (`GET /flights?from=WAW&to=LIS&date=...`)
- [ ] Global error handling (RFC 9457 `ProblemDetail`)
- [ ] Users, Spring Security, JWT
- [ ] Reservations: seat holding, optimistic locking, expiry scheduler
- [ ] Pricing engine (fare classes, taxes, price snapshot on reservation)
- [ ] Payments with idempotency keys
- [ ] Events: transactional outbox → Kafka → e-mail (Mailpit)
- [ ] Check-in and signed boarding passes
- [ ] Tests with Testcontainers (written alongside features)
- [ ] OpenAPI docs
- [ ] Frontend (React + Vite + TypeScript)

Detailed staging, acceptance criteria and open questions: [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

### Later / optional

- [ ] Containerize the backend (`./gradlew bootBuildImage` or a `Containerfile`) and add it to `infrastructure.yaml`
- [ ] Add Kafka (KRaft) and Mailpit to the Podman pod
- [ ] Move credentials to Kubernetes `Secret` and configuration to `ConfigMap`
- [ ] Notifications, in increasing order of complexity:
  1. In-process events (`ApplicationEventPublisher` + `@TransactionalEventListener`)
  2. Outbox pattern + Kafka, still inside the monolith
  3. Separate `notification-service` consuming events from Kafka
- [ ] Free-tier deployment (local-first, cloud-ready)
- [ ] Kubernetes deployment

## Notifications (design notes)

Notifications go to the user who made the reservation (events carry the e-mail address, so the consumer needs no extra lookups). Planned triggers:

- Reservation confirmed after payment
- Payment reminder before the hold expires
- Reservation expired and seat released
- Flight delayed or cancelled (notifies all passengers of the flight)
- Pre-departure reminder

In development, e-mails would be sent to a local SMTP sink such as Mailpit.
