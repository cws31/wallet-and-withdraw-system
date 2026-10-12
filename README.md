<div align="center">

# Wallet and Withdrawal System

**A backend-driven rewards wallet and withdrawal application**

Built with Java · Spring Boot · MySQL · React

[Features](#features) · [Architecture](#architecture) · [Getting Started](#getting-started) · [API](#api-overview) · [Security](#security)

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql&logoColor=white)
![React](https://img.shields.io/badge/React-JavaScript-61DAFB?logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-Frontend-646CFF?logo=vite&logoColor=white)

</div>

---

> **Project status:** Development / demonstration project. Verify current test results and deployment configuration before treating features as production-ready. This repository does not claim a live payout-provider integration.

## About

Wallet and Withdrawal System is a full-stack rewards wallet application built around a Spring Boot REST API and a React frontend. The backend is the source of truth for wallet balances, transaction ledger entries, payout configuration, withdrawal requests, and financial audit operations.

## Features

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>Wallet &amp; Ledger</h3>
      <ul>
        <li>VEs, SVEs, Gems, Tokens, and Spins</li>
        <li>Wallet balances, summaries, and transaction history</li>
        <li>Ledger entries for wallet changes</li>
        <li>Server-side balance and ownership validation</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>Withdrawals &amp; Payouts</h3>
      <ul>
        <li>Backend-provided payout methods and options</li>
        <li>Withdrawal request and status management</li>
        <li>Idempotency controls against duplicate requests</li>
        <li>Administrative processing and cancellation flows</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>Security &amp; Risk Controls</h3>
      <ul>
        <li>JWT authentication and role-based authorization</li>
        <li>BCrypt password hashing and request validation</li>
        <li>Redis-backed rate limiting</li>
        <li>Fraud/risk evaluation and audit records</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>Financial Integrity</h3>
      <ul>
        <li>Wallet-versus-ledger reconciliation</li>
        <li>Audit-friendly transaction history</li>
        <li>Flyway-managed database migrations</li>
        <li>Health and metrics endpoints through Actuator</li>
      </ul>
    </td>
  </tr>
</table>

## Technology Stack

| Area | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.5.16, Maven |
| API | Spring Web, REST, Springdoc OpenAPI |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL 8.x, Flyway |
| Security | Spring Security, JWT (JJWT), BCrypt |
| Rate limiting | Redis |
| Frontend | React, JavaScript, Vite |
| Styling and routing | Tailwind CSS, React Router |
| HTTP client | Axios |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc |

## Architecture

```text
┌──────────────────────────────┐
│       React Frontend         │
│        Vite / Browser        │
└──────────────┬───────────────┘
               │ HTTP + JWT
               ▼
┌──────────────────────────────┐
│       Spring Security        │
│ Authentication / Access     │
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│       REST Controllers       │
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│      Application Services    │
│ Auth · Wallet · Withdrawals  │
│ Payouts · Risk · Reconcile   │
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│ Spring Data JPA / Hibernate  │
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│            MySQL             │
└──────────────────────────────┘

Redis     → Rate limiting
Flyway    → Database migrations
Actuator  → Health and metrics
```

**Core principle:** The frontend is a client, not the authority. Wallet balances, payout values, ownership, and withdrawal eligibility must be validated by the backend using persisted state and server-side rules.

## Repository Structure

```text
wallet-and-withdrawal-system/
├── backend/                  # Spring Boot application, tests, migrations
├── frontend/                 # React / Vite application
├── docs/                     # Architecture and technical documentation
├── postman/                  # Postman API collection
├── API_DOCUMENTATION.md      # API endpoint reference
├── README.md
└── .gitignore
```

## Getting Started

### Prerequisites

- Java 21
- MySQL 8.x
- Redis, for configured rate-limiting functionality
- Node.js compatible with the frontend dependencies
- npm

Check your installed versions:

```bash
java -version
node --version
npm --version
```

### 1. Clone the repository

```bash
git clone https://github.com/cws31/wallet-and-withdrawal-system.git
cd wallet-and-withdrawal-system
```

### 2. Configure the backend

If `backend/.env.example` exists, copy it to `backend/.env` and fill in local values:

```bash
cp backend/.env.example backend/.env
```

Configure the database URL and credentials, JWT secret and expiration, CORS origins, and Redis settings. Create the local database and make sure required services are available.

> Never commit `.env` files, passwords, JWT secrets, or database URLs containing credentials. Confirm that variable names in the environment file match those expected by the application configuration.

### 3. Start the backend

**macOS / Linux**

```bash
cd backend
./mvnw spring-boot:run
```

**Windows**

```bat
cd backend
mvnw.cmd spring-boot:run
```

The default API port is `8080`, unless overridden by configuration. Flyway applies database migrations at startup when enabled and correctly configured.

### 4. Start the frontend

Open a second terminal:

```bash
cd frontend
npm install
```

If `frontend/.env.example` exists, copy it to `.env`:

```bash
cp .env.example .env
```

Set `VITE_API_BASE_URL` to the backend base URL (for example, `http://localhost:8080`) and start the development server:

```bash
npm run dev
```

Open the local URL printed by Vite. If frontend and backend use different origins, configure the backend CORS allowlist accordingly.

## API Overview

The following are representative endpoint groups. Check `API_DOCUMENTATION.md` for exact routes, request/response schemas, and permissions.

| Area | Example endpoints |
|---|---|
| Authentication | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Wallet | `GET /api/wallet`, `GET /api/wallet/transactions`, `GET /api/wallet/summary` |
| Payout configuration | `GET /api/payouts/configuration` |
| Withdrawals | `POST /api/withdrawals`, `GET /api/withdrawals` |
| Health | `GET /actuator/health` |
| OpenAPI | `GET /v3/api-docs` |

Protected endpoints require the configured authentication mechanism. Administrative operations require the appropriate role and authorization.

## Database Migrations

Flyway migration scripts are located in:

```text
backend/src/main/resources/db/migration/
```

Use a new versioned migration for subsequent schema changes. Avoid editing migrations that have already been applied to a shared database.

## Testing

Run backend tests from `backend/`:

```bash
./mvnw test
```

On Windows:

```bat
mvnw.cmd test
```

Some integration tests may require a reachable MySQL and Redis instance with valid configuration.

Run frontend checks from `frontend/`:

```bash
npm ci
npm run lint
npm run build
```

Report test results only after running the checks against the commit being evaluated. The existence of a CI workflow does not by itself mean the latest run passed.

## Security Notes

- Never commit credentials, JWT secrets, tokens, or real user data.
- Use strong, unique secrets outside local development.
- Keep wallet credit/debit and administrative endpoints protected.
- Validate wallet operations, payout options, and withdrawal requests on the server.
- Restrict CORS to the intended frontend origins.
- Avoid exposing database and Redis ports publicly.
- Ensure logs do not contain passwords, tokens, or sensitive payout details.

## Documentation

- [API Documentation](API_DOCUMENTATION.md)
- [Architecture](docs/architecture.md)
- [Database Design](docs/database.md)
- [Wallet Accounting](docs/wallet-accounting.md)
- [Withdrawal Flow](docs/withdrawal-flow.md)
- [Idempotency](docs/idempotency.md)
- [Security](docs/security.md)
- [Rate Limiting](docs/rate-limiting.md)
- [Reconciliation](docs/reconciliation.md)
- [Testing Guide](docs/testing.md)
- [Postman Collection](postman/)

> Check that each linked document exists at the listed path in your repository. Remove or update links that do not match the actual filenames.

## Current Limitations

This is a development and demonstration project and has not been independently certified for production financial use. A payout configuration or job queue does not by itself establish a live payout-provider integration. Real-money processing would require a verified provider integration, secure credentials, callback validation, retries, reconciliation, and appropriate operational controls.

## Contributing

Keep changes focused and reviewable. Update tests and documentation when changing wallet accounting, withdrawal transitions, API contracts, security controls, or database schema. Never commit secrets or local build artifacts.
