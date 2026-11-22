# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project overview

`honey-api` is a Spring Boot 4.1 REST API (Java 21, Gradle) backed by PostgreSQL, with schema managed by Flyway. Package root: `in.co.sivalokam.honey`.

The only feature today is **registrations**: `POST /api/v1/registrations` stores a person's name, phone, optional email and city, and consent timestamp. Phone numbers are normalized to `+91XXXXXXXXXX` and are unique.

## Commands

Use the Gradle wrapper (`./gradlew`), not a system Gradle.

- Build: `./gradlew build`
- Run tests: `./gradlew test` (tests use Testcontainers, so Docker must be running)
- Run locally: `./gradlew bootRun` (needs PostgreSQL, see below)
- Build the deployable jar without tests: `./gradlew bootJar -x test`

## Local database

`src/main/resources/application.properties` points at `localhost:5432/honey_db` with user `honey`. Start a matching PostgreSQL instance (e.g. Docker) before `bootRun`. Flyway runs migrations from `src/main/resources/db/migration` on startup.

Configuration values that come from the environment:

- `DB_USER` / `DB_PASSWORD` (defaults are `honey` / `honey_db`)
- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` (default empty)

Never hard-code secrets in `application.properties`. The file currently contains commented-out credentials; do not copy them anywhere else or add new ones.

## Code layout

- `HoneyApiApplication` — entry point.
- `config/` — app-wide beans (`ClockConfig`) and the `ApiExceptionHandler`.
- `registrations/` — feature package: `RegistrationController` (REST), `RegistrationService` (business logic, phone normalization, duplicate handling), `RegistrationRepository` (Spring Data JPA), `Registration` (JPA entity), `RegistrationRequest` / `RegistrationResponse` (DTOs), `DuplicateRegistrationException`.
- `src/main/resources/db/migration/` — Flyway migrations, named `V<n>__<description>.sql`.
- `src/test/` — `HoneyApiApplicationTests` (context load) with `TestcontainersConfiguration` providing a `postgres:latest` container via `@ServiceConnection`.

## Conventions

- Keep time-dependent code on the injected `Clock` bean (see `ClockConfig`) so it stays testable.
- Use `jakarta.*` imports (Jakarta EE, not `javax.*`).
- Controllers take DTO records and return `ResponseEntity`; entities stay inside the service layer.
- Validation goes on request DTOs with Jakarta Validation annotations (`@Valid`).
- Schema changes go in a new Flyway migration. Never edit an existing `V*__` file that may have run. `ddl-auto` is `validate`, so entity changes must match the migrations.
- Keep `spring.jpa.open-in-view=false`.

## Deployment

`.github/workflows/gradle.yml` runs on every push to `main` (and manually): it builds `bootJar` with tests skipped, then copies the jar over SSH to the VPS as `/root/honey-api/app.jar.new`. The workflow uses repository secrets (`VPS_SSH_KEY`, `VPS_HOST`, `VPS_PORT`, `VPS_USER`). Do not change the deploy target or secrets handling without asking.

## Git

Default branch is `main`. Commit messages in this repo are short, imperative summaries (e.g. "Refactor Gradle workflow for Spring Boot deployment").
