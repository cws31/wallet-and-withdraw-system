# Wallet and Withdrawal System

A full-stack rewards-wallet application built around a Spring Boot REST API and a React frontend. The backend is the source of truth for wallet balances, ledger entries, payout configuration, withdrawal requests, and financial audit operations.

> **Project status:** Development/demo application. Review the current test results and deployment configuration before treating any feature as production-ready. No production payout-provider integration is claimed by this repository.

## Contents

- [Features](#features)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [Repository structure](#repository-structure)
- [Prerequisites](#prerequisites)
- [Local setup](#local-setup)
- [API overview](#api-overview)
- [Database migrations](#database-migrations)
- [Testing](#testing)
- [Security notes](#security-notes)
- [Documentation](#documentation)
- [Current limitations](#current-limitations)

## Features

### Wallet and ledger

- Supports VEs, SVEs, Gems, Tokens, and Spins.
- Provides wallet balances, transaction history, and wallet summary APIs.
- Records wallet changes in a transaction ledger.
- Performs server-side balance and ownership checks for wallet operations.

### Payouts and withdrawals

- Exposes payout configuration from the backend rather than relying on frontend-defined payout values.
- Supports withdrawal requests and withdrawal status management.
- Includes idempotency handling to help prevent duplicate withdrawal operations.
- Includes administrative processing, approval, rejection, and cancellation flows.
- Uses compensating ledger entries for applicable reversal flows rather than deleting financial history.

### Security and operational controls

- Spring Security with JWT-based authentication and role-based authorization.
- BCrypt password hashing and request validation.
- Redis-backed rate limiting.
- Fraud/risk evaluation and audit records.
- Wallet-versus-ledger reconciliation for administrative investigation.
- Spring Boot Actuator health and metrics endpoints, plus request/correlation tracing support.

## Technology stack

| Area | Technology |
|---|---|
| Backend language | Java 21 |
| Backend framework | Spring Boot 3.5.16 |
| Build | Maven / Maven Wrapper |
| API | Spring Web, REST, Springdoc OpenAPI |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL 8.x |
| Schema migrations | Flyway |
| Authentication | Spring Security, JWT (JJWT) |
| Password hashing | BCrypt |
| Rate limiting | Redis |
| Frontend | React 19, JavaScript, Vite |
| Styling | Tailwind CSS 4 |
| HTTP client and routing | Axios, React Router |
| Backend tests | JUnit 5, Mockito, Spring Boot Test, MockMvc, integration tests |

## Architecture

```text
Browser (React / Vite)
        |
        | HTTP + JWT
        v
Spring Security filters
        |
        v
REST controllers
        |
        v
Application/domain services
  |       |        |          |
 Auth   Wallet   Withdrawal  Fraud / Reconciliation
          |         |
          v         v
      JPA repositories / audit / idempotency
                    |
                    v
                  MySQL

Redis: rate limiting
Flyway: database schema migrations
Actuator: health and metrics
```

The frontend is a client only. It must not be trusted to supply authoritative wallet balances, withdrawal costs, ownership, or payout values. The backend should resolve and validate those values from persisted state and server-side rules.

## Repository structure

```text
wallet-and-withdrawal-system/
├── backend/                 # Spring Boot application, tests and migrations
├── frontend/                # React/Vite user interface
├── docs/                    # Architecture and domain/operations documentation
├── postman/                 # Postman API collection
├── API_DOCUMENTATION.md     # API endpoint reference
├── README.md                # Project overview and setup
└── .gitignore
```

## Prerequisites

Install the following before starting locally:

- Java 21
- MySQL 8.x
- Redis, for the configured rate-limiting functionality
- Node.js compatible with the Vite version in `frontend/package.json`
- npm

You can check installed versions with:

```bash
java -version
node --version
npm --version
```

## Local setup

### 1. Get the source code

```bash
git clone https://github.com/cws31/wallet-and-withdrawal-system.git
cd wallet-and-withdrawal-system
```

Replace the placeholder URL with the HTTPS or SSH URL shown on your GitHub repository page.

### 2. Configure the backend

Create a local environment file from the example:

```bash
cp backend/.env.example backend/.env
```

Edit `backend/.env` with your local MySQL connection, database username/password, a strong JWT secret, CORS origins, and Redis settings. Never commit the real `.env` file or production credentials.

Create an empty MySQL database for local development and ensure the configured Redis instance is available. Confirm that `backend/src/main/resources/application.properties` reads the environment variables expected by `backend/.env.example`; environment variable names must match exactly.

### 3. Start the backend

macOS/Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell or Command Prompt:

```bat
cd backend
mvnw.cmd spring-boot:run
```

The default local API port is configured through `SERVER_PORT` (the example uses `8080`). Flyway applies the database migrations during application startup when correctly configured.

### 4. Start the frontend

Open a second terminal from the repository root:

```bash
cd frontend
npm install
```

Create the frontend environment file:

```bash
cp .env.example .env
```

Set `VITE_API_BASE_URL` to the backend base URL, for example `http://localhost:8080`, then start Vite:

```bash
npm run dev
```

Open the local URL printed by Vite. If the frontend and backend run on different origins, configure the backend CORS allowlist accordingly.

## API overview

The exact request and response schemas are documented in [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md). Main API groups include:

| Area | Example endpoints |
|---|---|
| Authentication | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Wallet | `GET /api/wallet`, `GET /api/wallet/transactions`, `GET /api/wallet/summary` |
| Payout configuration | `GET /api/payouts/configuration` |
| Withdrawals | `POST /api/withdrawals`, `GET /api/withdrawals`, `GET /api/withdrawals/{withdrawalId}` |
| Admin withdrawal queue | `GET /api/withdrawals/admin?page=1&limit=20` (admin only) |
| Admin withdrawal actions | `PATCH /api/withdrawals/{withdrawalId}/processing`, `/approve`, `/reject`, and `/cancel` (see API docs for exact paths and permissions) |
| Reconciliation | `GET /api/admin/reconciliation/wallet/{walletId}` |
| Health | `GET /actuator/health` |
| OpenAPI | `GET /v3/api-docs` and the configured Swagger UI route |

Protected endpoints require the authentication mechanism configured by the backend. Administrative endpoints require appropriate authorization. Consult the API documentation and security configuration before using the endpoints.

## Database migrations

Flyway migration scripts are maintained under:

```text
backend/src/main/resources/db/migration/
```

Do not manually edit a migration that has already been applied to a shared database. Add a new versioned migration for subsequent schema changes. See [`docs/database.md`](docs/database.md).

## Testing

Run backend tests from the `backend/` directory:

```bash
./mvnw test
```

On Windows:

```bat
mvnw.cmd test
```

Some integration tests may require a reachable MySQL/Redis instance and correctly configured test properties. Run the frontend production build and lint checks from `frontend/`:

```bash
npm ci
npm run build
npm run lint
```

Only report test counts after running the commands against the current commit. A previously recorded test count is not a substitute for a fresh test result.

## Continuous integration

GitHub Actions is configured in [`.github/workflows/ci.yml`](.github/workflows/ci.yml). On pushes to `main`/`master`, pull requests, and manual runs, it:

- runs backend Maven verification/tests with Java 21, MySQL 8.4, and Redis 7;
- installs the locked frontend dependencies with `npm ci`;
- runs frontend lint and a production build.

The workflow reports the result of the current commit. Do not describe the build as passing until the Actions run completes successfully.

## Security notes

- Never commit `.env` files, passwords, JWT secrets, database URLs containing credentials, access tokens, or real user data.
- Keep `backend/.env.example` and `frontend/.env.example` limited to placeholders and non-sensitive local defaults.
- Use a strong, unique JWT secret outside local development.
- Do not expose database or Redis ports publicly for a demo deployment.
- Restrict CORS origins and administrative endpoints to the intended environments and roles.
- Wallet credit/debit endpoints and reconciliation/admin APIs must remain protected; do not expose them as public user operations.
- Review logging to ensure tokens, passwords, payout details, and other sensitive data are not written to logs.

## Documentation

- [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md) — endpoint reference
- [`docs/architecture.md`](docs/architecture.md) — system architecture
- [`docs/database.md`](docs/database.md) — database design and migrations
- [`docs/wallet-accounting.md`](docs/wallet-accounting.md) — wallet and ledger concepts
- [`docs/withdrawal-flow.md`](docs/withdrawal-flow.md) — withdrawal lifecycle
- [`docs/idempotency.md`](docs/idempotency.md) — idempotency behavior
- [`docs/security.md`](docs/security.md) — security controls
- [`docs/rate-limiting.md`](docs/rate-limiting.md) — rate-limiting configuration
- [`docs/reconciliation.md`](docs/reconciliation.md) — wallet/ledger reconciliation
- [`docs/testing.md`](docs/testing.md) — test guidance
- [`postman/`](postman/) — API request collection

## Current limitations

- This repository is a development/demo project and has not been independently certified for production financial use.
- A payout-job/worker/provider abstraction does not by itself establish a live payout integration. Configure and validate a real provider, credentials, callbacks, reconciliation, retries, and operational controls before processing real payouts.
- Test results and deployment behavior depend on the current code and environment; verify them against the commit you intend to publish.

## Contributing

Keep changes focused and reviewable. Update tests and relevant documentation when changing wallet accounting, withdrawal transitions, security rules, API contracts, or database schema. Never include secrets or local build artifacts in commits.
