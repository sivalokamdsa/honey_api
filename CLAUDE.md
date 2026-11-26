# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`honey-api` is a Spring Boot 4.1 REST API (Java 21, Gradle) backed by PostgreSQL, with schema managed by Flyway. Package root: `in.co.sivalokam.honey`.

Features: registrations, login, product catalog, checkout with Razorpay payments, and Razorpay webhooks. The endpoints are listed in the API reference below.

## API reference

All JSON uses camelCase field names. Money is in paise. Error responses are `ProblemDetail` objects with `status`, `title`, `detail`, and `errors` (a field-to-message object, empty when there are none). Bean-validation failures return 400 with `title: "Validation failed"` and one entry per field in `errors`.

### Registrations

**`POST /api/v1/registrations`** — creates a registration.

Request:

```json
{
  "fullName": "Asha Rao",
  "phone": "+91 98765 43210",
  "email": "asha@example.com",
  "city": "Pune",
  "password": "secret123",
  "consent": true
}
```

- `fullName`: required, 2–100 characters.
- `phone`: required, a 10-digit Indian mobile number. Accepts `+91`, `91`, or `0` prefixes and spaces or dashes. Stored as `+91XXXXXXXXXX`.
- `email`: optional, valid email, max 254 characters. Stored lowercase.
- `city`: optional, max 80 characters.
- `password`: required, 8–72 characters. Stored only as a BCrypt hash.
- `consent`: required, must be `true`.

Response `201 Created`, with `Location: /api/v1/registrations/{id}`:

```json
{ "id": "3f2c1e0a-…", "createdAt": "2026-01-01T00:00:00Z" }
```

Errors:
- `400`: validation failure, or a password longer than 72 bytes (`errors.password`).
- `409` (`title: "Already registered"`): the phone number is already registered.

### Login

**`POST /api/v1/auth/login`** — checks a phone number and password. It issues no token or session.

Request:

```json
{ "phone": "9876543210", "password": "secret123" }
```

Response `200 OK`:

```json
{ "id": "3f2c1e0a-…", "fullName": "Asha Rao", "phone": "+919876543210" }
```

Errors:
- `400`: `phone` or `password` is missing.
- `401` with `detail: "Invalid phone or password"`: unknown phone, wrong password, or a registration created before passwords existed. The message is the same in all three cases.

### Products

**`GET /api/v1/products`** — lists the catalog, sorted by `id`. Public.

Response `200 OK`:

```json
[
  {
    "id": "honey-1l",
    "name": "Pure Honey — 1 Litre",
    "description": "Raw, unprocessed forest honey in a 1 litre jar.",
    "pricePaise": 45000,
    "mrpPaise": 50000,
    "inStock": true
  }
]
```

### Checkout

**`POST /api/v1/checkout/orders`** — prices the cart from the database, checks stock, creates a Razorpay order, and stores an internal order with status `created`.

Request:

```json
{
  "items": [{ "productId": "honey-1l", "quantity": 2 }],
  "customer": { "name": "Asha Rao", "phone": "9876543210", "email": "asha@example.com" }
}
```

- `items`: 1–20 entries. Each `productId` must be unique within the cart. Each `quantity` is 1–20.
- `customer.name`: 2–100 characters. `customer.phone`: same rules as registration. `customer.email`: optional.
- Client-supplied prices are ignored.

Response `201 Created`:

```json
{
  "orderId": "8a1b…",
  "status": "created",
  "razorpayOrderId": "order_XXXX",
  "razorpayKeyId": "rzp_test_…",
  "currency": "INR",
  "subtotalPaise": 90000,
  "discountPaise": 0,
  "totalPaise": 90000,
  "items": [
    { "productId": "honey-1l", "name": "Pure Honey — 1 Litre", "unitPricePaise": 45000, "quantity": 2, "lineTotalPaise": 90000 }
  ]
}
```

The client passes `razorpayOrderId` and `razorpayKeyId` to Razorpay Checkout. Discounts are not implemented yet, so `discountPaise` is always 0.

Errors:
- `400`: validation failure, or `errors` keyed by `items[i].productId` with `Unknown product`, `<name> is out of stock`, or `Each product can appear only once`.
- `502`: Razorpay order creation failed (`detail: "We couldn't start your payment. Please try again."`).

**`POST /api/v1/checkout/verify`** — checks the Razorpay payment signature and marks the order paid. Safe to call repeatedly for the same payment.

Request:

```json
{
  "orderId": "8a1b…",
  "razorpayOrderId": "order_XXXX",
  "razorpayPaymentId": "pay_XXXX",
  "razorpaySignature": "hex-hmac-sha256"
}
```

Response `200 OK`:

```json
{ "status": "paid", "orderId": "8a1b…", "paymentId": "pay_XXXX" }
```

Errors:
- `400`: validation failure; `razorpayOrderId` does not match the order; the signature does not match (`errors.razorpaySignature`); or the payment amount does not match the order total.
- `404`: order not found.
- `409`: the order is already paid by a different payment, or this payment belongs to a different order.

### Razorpay webhook

**`POST /api/v1/webhooks/razorpay`** — receives Razorpay events. Called by Razorpay, not by the frontend. The body is the raw Razorpay event JSON. The `X-Razorpay-Signature` header is an HMAC-SHA256 of the raw body, using the webhook secret.

- Only `payment.captured` is acted on. It marks the matching order paid, using the same path as `/checkout/verify`.
- Other events, and events for unknown orders, return 200 and are ignored.
- Failures to apply a valid payment (for example, an amount mismatch) are logged and still return 200, so Razorpay does not retry.

Response `200 OK` with an empty body. Response `400` (`detail: "Invalid webhook signature"`) when the signature header is missing or wrong.

### Health

Spring Boot Actuator exposes `GET /actuator/health` and `GET /actuator/info`.

## Commands

Use the Gradle wrapper (`./gradlew`), not a system Gradle.

- Build: `./gradlew build`
- Run all tests: `./gradlew test` (Testcontainers starts `postgres:latest`, so Docker must be running)
- Run one test class: `./gradlew test --tests 'in.co.sivalokam.honey.checkout.CheckoutServiceTest'`
- Run one test method: `./gradlew test --tests 'in.co.sivalokam.honey.checkout.CheckoutServiceTest.methodName'`
- Run locally: `./gradlew bootRun` (needs PostgreSQL, see below)
- Run locally against a disposable database: run `TestHoneyApiApplication` (in `src/test`) from the IDE. It starts the app with a Testcontainers PostgreSQL, so Docker must be running.
- Deployable jar without tests: `./gradlew bootJar -x test`
- No lint or format task is configured (no Checkstyle or Spotless in `build.gradle`).

## Local database and configuration

`src/main/resources/application.properties` points at `localhost:5432/honey_db` with user `honey`. Start a matching PostgreSQL instance (e.g. Docker) before `bootRun`. Flyway runs migrations from `src/main/resources/db/migration` on startup.

The active config sets the username but no password. The password lines are commented out and reference `DB_USER` / `DB_PASSWORD`. The file currently contains commented-out credentials: do not copy them anywhere else or add new ones. Configure the password locally instead.

Configuration values that come from the environment:

- `DB_USER` / `DB_PASSWORD` (see above)
- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` (default empty)
- `RAZORPAY_BASE_URL` (default `https://api.razorpay.com`)

Never hard-code secrets in `application.properties`.

## Architecture

**Request path.** Controllers accept a DTO record annotated `@Valid` and return `ResponseEntity`. Services hold the logic and call Spring Data repositories. Business errors are thrown as `common/ApiException` with a status, a message, and an optional field-to-message `errors` map. `config/ApiExceptionHandler` turns those, validation failures, malformed JSON, and unexpected exceptions into `ProblemDetail` responses that all carry an `errors` property.

**Checkout** (`checkout/`):

- `CheckoutService.createOrder` never trusts client prices. It loads products from the database, rejects unknown, out-of-stock, or duplicate items with field-keyed errors, computes the subtotal in paise, and creates the Razorpay order through the `RazorpayClient` interface. `RestRazorpayClient` implements it using the `razorpayHttpClient` RestClient from `config/RazorpayConfig`. The internal `StoreOrder` is saved with status `CREATED`. Discounts are not implemented yet: `discountFor` always returns 0.
- Payment confirmation has two entry points that both end in `PaymentRecorder.recordPaid`:
  1. `/checkout/verify` checks the HMAC-SHA256 signature of `orderId|paymentId` with the key secret (`RazorpaySignatures`).
  2. `/webhooks/razorpay` handles `payment.captured` events. It checks the HMAC of the raw body with the webhook secret. This is a separate secret from the API key secret.
- `recordPaid` is `@Transactional`. It locks the order row (`findByIdForUpdate`), is idempotent per `razorpay_payment_id`, requires the amount to equal the order total, and then marks the order `PAID`.
- The webhook catches `ApiException` from `recordPaid` and still returns 200, so Razorpay does not retry payments the app cannot apply.
- The webhook takes `@RequestBody String` on purpose: the signature is computed over the exact bytes. Do not switch it to a deserialized body.
- Money is stored as `int` paise everywhere. Currency is INR.

**Shared helpers.** `common/PhoneNumbers` normalizes phone numbers for both registrations and checkout customers.

**Schema** (`db/migration/`): V1 registrations, V2 products, V3 orders, order items, and payments. Unique constraints on `orders.razorpay_order_id` and `payments.razorpay_payment_id` back the idempotency logic.

**JSON.** The code uses Jackson 3 (`tools.jackson.databind`), not `com.fasterxml`.

**Tests.** Unit tests for `CheckoutService` and `RazorpaySignatures` live under `checkout/`. `HoneyApiApplicationTests` loads the full context against Testcontainers through `TestcontainersConfiguration` (`@ServiceConnection`).

## Conventions

- Keep time-dependent code on the injected `Clock` bean (see `ClockConfig`) so it stays testable.
- Use `jakarta.*` imports (Jakarta EE, not `javax.*`).
- Controllers take DTO records and return `ResponseEntity`. Entities stay inside the service layer. Known exception: `RazorpayWebhookController` calls `StoreOrderRepository` and works with `StoreOrder` directly. Do not copy that pattern into new code.
- Validation goes on request DTOs with Jakarta Validation annotations (`@Valid`).
- Schema changes go in a new Flyway migration (V4 and up). Never edit an existing `V*__` file that may have run. `ddl-auto` is `validate`, so entity changes must match the migrations.
- Keep `spring.jpa.open-in-view=false`.
- Passwords are stored only as BCrypt hashes (`PasswordEncoder` bean in `config/PasswordConfig`). Never store or log plaintext. Login errors must stay generic ("Invalid phone or password").

## Deployment

`.github/workflows/gradle.yml` runs on every push to `main` (and manually). It builds `bootJar` with tests skipped, then copies the jar over SSH to the VPS as `/root/honey-api/app.jar.new`. The workflow uses repository secrets (`VPS_SSH_KEY`, `VPS_HOST`, `VPS_PORT`, `VPS_USER`). Do not change the deploy target or secrets handling without asking.

## Git

Default branch is `main`. Commit messages in this repo are short, imperative summaries (e.g. "Refactor Gradle workflow for Spring Boot deployment").
