# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Running the application
```bash
# Start PostgreSQL (required first)
docker compose up -d

# Run backend (Spring Boot)
./mvnw spring-boot:run
```

### Building and testing
```bash
# Run all tests (includes Spring Modulith boundary verification)
./mvnw test

# Full build
./mvnw clean install

# Run a single test class
./mvnw test -Dtest=ModularityTests

# Build OCI image
./mvnw spring-boot:build-image
```

### Required local secrets (not committed, must be created manually)
```bash
# .env (project root)
POSTGRES_PASSWORD=covoit

# src/main/resources/application-local.yaml
spring:
  datasource:
    password: covoit
app:
  jwt:
    secret: "CHANGE_ME_MIN_32_CHARS"
```

## Architecture

### Modular monolith (Spring Modulith + Hexagonal)

5 modules, each enforcing boundaries via `package-info.java` with `@ApplicationModule`:

| Module | Responsibility | Allowed dependencies |
|---|---|---|
| `identity` | Users, roles | — |
| `auth` | Register, login, JWT, refresh, logout | `identity` |
| `rides` | Ride publication, search, seat management | — |
| `bookings` | Booking request workflow | `rides` |
| `notifications` | Event listeners (mock log) | `rides`, `bookings` |

### Hexagonal structure per module
```
<module>/
  domain/          # Aggregates, value objects, domain events, invariants
  application/     # Use cases, services, commands, ports
  adapters/in/     # REST controllers, event listeners
  adapters/out/    # JPA persistence, external service adapters
```

### Cross-module communication
All inter-module communication goes through **domain events** (never direct service calls across module boundaries). Events are:
1. Published via `ApplicationEventPublisher`
2. Persisted to the `event_publication` table (outbox pattern, at-least-once delivery)
3. Routed to Artemis JMS queues for externalized events (tagged `@Externalized`)

Externalized event queues:
- `auth.UserRegisteredEvent`
- `rides.RidePublishedEvent`, `rides.RideCanceledEvent`
- `bookings.BookingRequestedEvent`, `bookings.BookingAcceptedEvent`, `bookings.BookingRejectedEvent`, `bookings.BookingCanceledEvent`

### Key patterns
- **Optimistic locking** on `rides` table (`version` column) for concurrent seat reservation — raises `ConcurrentUpdateException` on conflict
- **Opaque refresh tokens** — token hash stored in DB, rotated on each use, tracked per device
- **Phone validation** via libphonenumber before user registration
- `ModularityTests.java` verifies all module boundaries on each test run — do not bypass

### Branch context: `feat/extract-auth-microservice`
This branch is Phase 1 of an auth microservice extraction:
- Events externalized via Artemis (embedded JMS, `persistent: false`)
- `TokenValidator` interface introduced to decouple JWT validation from generation
- Phase 2 (future): extract `identity` + `auth` into a standalone `auth-service`
- See `docs/EXTRACTION_AUTH_PLAN.md` for the full roadmap

### Database
PostgreSQL 16 via Docker. Flyway manages 7 versioned migrations in `src/main/resources/db/migration/`. Hibernate DDL is set to `validate` — schema changes must go through Flyway migrations.

### API testing
Bruno collection in `/bruno` folder with pre-configured requests and automatic JWT token management. Test credentials: `test@test.com / Password1234xxx` (driver), `passager@test.com / Password1234xxx` (passenger).