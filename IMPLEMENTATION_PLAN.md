# Implementation Plan — Flight Booking Backend

A portfolio project demonstrating mid/senior backend engineering (plus a thin TypeScript client), built by one developer, running fully local and cost-free.

> This file is the working plan. The README stays the public-facing document; this is the engineering breakdown behind it.

---

## 1. Current state of the repository

### What the README claims vs. what the code does

| Item | README says | Actual code |
|---|---|---|
| Architecture | Package-by-module monolith | Multi-module Gradle, hexagonal: `domain`, `application`, `adapter:web`, `adapter:persistence`, `bootstrap` |
| Java / Spring | Java 21, Boot 3.x | Toolchain **25**, Boot **4.1.1**, Kotlin 2.4.20 |
| PostgreSQL | 16 | **18** in `infra/infrastructure.yaml` |
| Package | `com.yourname.flights` | `dev.maxaure.flightbooking` |
| Status | "early development" | Only `Main.kt` and `application.yaml` exist; all modules are empty shells |


## 2. Locked decisions

| Area | Decision |
|---|---|
| Architecture | Hexagonal multi-module Gradle: `domain` → `application` → `adapter:*` → `bootstrap` |
| Persistence | Pure Kotlin domain + JPA entities in `adapter:persistence` with an explicit mapping layer |
| Schema | Flyway owns the schema; single clean history starting at `V1`; Hibernate `ddl-auto: validate` |
| Auth | Spring Security + JWT, `ROLE_CUSTOMER` / `ROLE_ADMIN`, ownership checks |
| Depth features | All four: payments + idempotency, events + outbox + Kafka + Mailpit, pricing engine, check-in/boarding pass |
| Async | Single-node Kafka (KRaft) in the Podman pod; transactional outbox relayed by a scheduled publisher |
| Hosting | Local-first with Podman; `Containerfile` + env-based config + k8s manifests so it *can* deploy on a free tier. No paid infrastructure |
| Frontend | Thin React + Vite + TypeScript client: search, seat map, hold countdown, login, my bookings |
| Docs | OpenAPI (springdoc), `.http` collection, ADR section in the README |
| Money | Value object; never `Double` |
| Passwords | BCrypt (boring and safe) unless changed in §7 |

### Frontend framework rationale

Web search returned no usable Polish market statistics (NoFluffJobs and justjoin.it render client-side, search engines were blocked). Do **not** invent numbers.

Verified from live pages: NoFluffJobs' Fullstack filters promote **React, Angular, JavaScript, TypeScript**; a live EPAM senior fullstack listing asks for "Node.js, React or Angular". Conclusion: React and Angular are the two that matter in Poland; React is the safe default.

For a backend-heavy portfolio the frontend framework is the least important choice. Pick React + Vite for speed and state clearly in the README that the UI exists to exercise the API.

---

## 3. Module and package layout

```
domain/                pure Kotlin: models, invariants, value objects (Money, SeatNumber, Email),
                       domain exceptions, domain events. No Spring, no Jakarta, no JPA.
application/           use cases + ports (interfaces) + transaction boundaries.
                       Depends on domain only.
adapter/web/           REST controllers, DTOs, problem+json advice, security config, springdoc.
adapter/persistence/   JPA entities, Spring Data repositories, mappers, search specifications.
adapter/kafka/         outbox relay, Kafka publisher + consumer, Mailpit SMTP sender.
bootstrap/             Main, application.yaml, bean wiring, integration test infra.
```

Dependency direction: `bootstrap → adapter:* → application → domain`. Adapters may not depend on each other. `domain` and `application` must compile with only `kotlin-stdlib`.

Enforced by ArchUnit tests in `bootstrap` (or a dedicated `architecture-test` source set):
- `domain` must not depend on `org.springframework..` or `jakarta.persistence..`
- `application` must not depend on adapters
- adapters must not depend on each other
- controllers must not reach repositories directly

---

## 4. Stages

### Stage 0 — Fix the foundation

A single sitting to make the project build and the docs truthful.

- Add ArchUnit and its rules; add a Testcontainers-based integration test harness (Postgres container shared via a base class).

**Done when:** `./gradlew build` passes, ArchUnit rules run, `podman kube play` brings up Postgres + Kafka + Mailpit, and the README contains no false claims.

---

### Stage 1 — Catalogue ingestion and search

The "boring but well done" part.

- `V1__init.sql`: `airport`, `flight`, `fare_class`, `flight_seat`, plus indexes for search.
- Catalogue ingestion (simulated feed + admin overrides), admin-only: airport and flight writes stand in for an upstream airline inventory feed (a PSS / NDC-style source), not the real-world authoring path. A `FlightFeedPort` plus a scheduled importer is the documented next step; until then admin endpoints act as the feed.
- Flights are never authored by customers: the thin UI (Stage 9) has no admin screens, so demo data comes from the `dev` seed or the admin section of the `.http` collection.

- Automatic seat generation per flight and fare class (rows × letters).
- `GET /api/v1/flights?from=WAW&to=LIS&date=&cabin=&sort=&page=&size=`
  - A `FlightSearchCriteria` object, not a long parameter list.
  - Offset pagination first; consider keyset pagination in an ADR.
  - Stable ordering at minimum by `departure_at, id`.
- Demo seed for the `dev` profile: ~50 airports, ~500 flights over 30 days.

**Tests:** filter combinations, pagination boundaries, invalid input → 400, empty result shape, indexes exercised.

**Done when:** search response time is documented with `EXPLAIN ANALYZE` before/after the index in the README.

---

### Stage 2 — Pricing engine

- `Money` value object (amount + currency, scale 2, no `Double`), arithmetic and comparison.
- Fare-class multipliers, taxes/fees, optional per-flight price override.
- Price is **snapshotted onto the reservation** so later price changes do not rewrite history.

**Tests:** pure-domain unit tests covering the pricing table, rounding, currency mismatch rejection, zero/negative price rejection.

**Done when:** pricing is tested without a database, and the rule set is documented in `docs/adr` or the README.

---

### Stage 3 — Auth and users

- `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`.
- BCrypt password hashing; access token (short) + refresh token (longer); refresh token rotation.
- Spring Security filter chain, method security, `ROLE_CUSTOMER` / `ROLE_ADMIN`.
- `/api/v1/my/reservations` with ownership enforcement.

**Tests:** duplicate e-mail, weak password, wrong password, expired token, refresh rotation, accessing another user's reservation → 403.

**Done when:** all non-public endpoints require a token and ownership rules are covered by tests.

---

### Stage 4 — Reservations and the concurrency story *(flagship)*

- `POST /api/v1/reservations` holds seats: `AVAILABLE → HELD`, `expires_at = now + 15 min`.
- **Optimistic locking** on `flight_seat.version`; the losing-conflict path and retry policy are documented.
- `POST /api/v1/reservations/{id}/confirm` and `/cancel`.
- Scheduled job releases expired holds in batches using `SKIP LOCKED`.
- **Concurrency proof:** a test firing e.g. 50 threads at a single last seat, asserting exactly one `201` and 49 conflicts. Capture the output into the README.

**Tests:** expiry actually releases seats, double confirm is idempotent, cancel releases seats, cross-user access denied, high-contention test.

**ADR candidate:** optimistic locking vs. `SELECT … FOR UPDATE` vs. `pg_advisory_xact_lock`, with a measured comparison.

**Done when:** the concurrency test is green and its output is visible in the README.

---

### Stage 5 — Payments and idempotency

- `payment` aggregate with the state machine `PENDING → AUTHORIZED → CAPTURED` and `FAILED` / `REFUNDED`.
- `PaymentGateway` port; in-memory fake for dev, Stripe test-mode adapter behind a profile flag (not required to run the app).
- `Idempotency-Key` header on charge/confirm:
  - store the key, a request hash and the response;
  - replay with the same key and body returns the stored response;
  - same key with a different body → `422`.
- Failed payment leaves the hold to expire naturally; no silent seat release.

**Tests:** replay returns identical response, conflicting payload rejected, gateway failure path, refund transition.

**Done when:** an idempotency test proves a double-click cannot charge twice.

---

### Stage 6 — Events, outbox, Kafka, e-mail

- `outbox_event` table written **in the same transaction** as the state change.
- Scheduled relay publishes to Kafka topic `reservation.events`.
- Consumer sends e-mail through SMTP to Mailpit.
- Events: `ReservationConfirmed`, `ReservationExpired`, `FlightDelayed` (admin-triggered), `CheckInOpen`.
- Consumer must be idempotent (dedupe by event id).

**Tests:** outbox written atomically with the state change, relay retries when the broker is down, consumer dedupe, mail lands in Mailpit.

**Design note:** keep the publisher transport-agnostic so a cloud profile can use outbox polling alone if managed Kafka is too expensive.

---

### Stage 7 — Check-in and boarding pass

- Check-in window: 24h to 2h before departure.
- One boarding pass per passenger; duplicate check-in rejected.
- Boarding pass carries a signed token (HMAC or JWT) rendered as a QR; `GET /api/v1/boarding-passes/{token}` verifies the signature.

**Tests:** window boundaries (exactly 24h, exactly 2h), duplicate check-in, tampered or expired token rejected.

---

### Stage 8 — Operational polish

- RFC 9457 `ProblemDetail` for every error; a trace id in responses.
- Structured JSON logs.
- Actuator health/readiness/Prometheus; basic request metrics.
- Multi-stage `Containerfile`, non-root user.
- `k8s/` manifests (or extend `infra/infrastructure.yaml`).
- OpenAPI with request/response examples; `.http` collection for the full booking flow.
- A "deploy free" README section: Fly.io/Railway/Neon, with the cost pitfalls stated explicitly.

**Done when:** one command starts the stack and one `.http` file demonstrates the whole journey.

---

### Stage 9 — Thin TypeScript UI

React + Vite + TypeScript + TanStack Query.

Screens: flight search → seat map → hold with countdown → login → pay → confirmation → boarding pass, plus "my bookings".

Rules: the client only calls the public API; no business logic in the browser; handle `409` and expired holds gracefully.

**Done when:** the booking flow is demoable end-to-end against the local stack.

---

### Stage 10 — Portfolio finishing

- 6 ADRs, each with the decision, the alternative, and what would change at scale:
  1. Hexagonal multi-module layout
  2. Optimistic vs. pessimistic locking (with measurements)
  3. Transactional outbox over in-process events
  4. Idempotency keys on payments
  5. Price snapshot on reservation
  6. Pure domain with a mapping layer
- README: architecture diagram, concurrency test output, `EXPLAIN ANALYZE` numbers, known limitations.
- A 2–3 minute screen recording or GIF of the booking flow.

**Done when:** a reviewer can understand the design from the README alone in under ten minutes.

---

## 5. What reviewers will actually look for

Ordered by impact:

1. A README with a clear architecture diagram and an honest "known limitations" list.
2. Proof of correctness under concurrency (test output, not claims).
3. ADRs that name trade-offs and alternatives.
4. Real tests: integration with Testcontainers, pure-domain unit tests, ArchUnit rules.
5. A reproducible one-command local setup.

Endpoint count ranks last. Depth beats breadth.

---

## 6. Risks

| Risk | Mitigation |
|---|---|
| Kafka on a free tier costs money | Transport-agnostic publisher; cloud profile can poll the outbox only |
| Optimistic locking retries can amplify load under contention | Measure both strategies; ADR with numbers |
| Scope creep across ten stages | Each stage ends with a working, tested slice |
| Podman + Testcontainers quirks | Document `DOCKER_HOST` and `TESTCONTAINERS_RYUK_DISABLED` (already in the README) |
| No commits yet | Commit after Stage 0 so the history starts from a clean, truthful base |

---

## 7. Open questions

These were raised but not yet answered; the plan assumes the defaults shown.

1. **Stage order** — keep pricing (Stage 2) before auth (Stage 3)? *Default: yes, as written.*
2. **Kafka implementation** — Apache Kafka KRaft vs. Redpanda (lighter, Kafka-API compatible). *Default: try Redpanda first; fall back to Kafka.*
3. **Password hashing** — BCrypt vs. Argon2. *Default: BCrypt.*
4. **Demo seed** — auto-seeded `dev` profile with realistic data. *Default: yes.*
5. **Repo layout** — `frontend/` inside this repo. *Default: yes, monorepo.*
