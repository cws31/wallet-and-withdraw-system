# VELoop Rewards — Wallet & Withdrawal Backend

> **Backend status:** FINAL BACKEND VERIFICATION COMPLETE  
> **Automated regression:** `257/257 PASS`  
> **Manual API verification:** Completed  
> **Current scope:** Backend implementation and verification

VELoop Rewards Backend is an independently developed Spring Boot backend for a secure, backend-controlled rewards wallet, payout configuration, withdrawal processing, fraud/abuse protection, idempotency, audit logging, reconciliation, rate limiting, and operational observability.

The backend is the **single source of truth** for financial and reward state. The client must never be trusted to provide wallet balances, authoritative withdrawal amounts, payout values, or ownership.

This is a development/demo system. It must not connect to VELoop production databases, credentials, or production services.

---

## 1. Project Objectives

The backend manages:

- VES
- SVES
- Gems
- Tokens
- Spins
- Wallet balances
- Wallet ledger transactions
- Reward credits and deductions
- Exchange-related balance changes
- Payout methods and payout options
- Withdrawal requests
- Withdrawal status
- Payout details
- User ownership/isolation
- Idempotency
- Fraud and abuse protection
- Rate limiting
- Audit records
- Reconciliation
- Payout-job processing architecture
- Health, metrics, and request tracing

The central rule is:

> **The frontend is only the interface. The backend owns the money logic.**

---

## 2. Technology Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| API | Spring Web / REST |
| Security | Spring Security |
| Authentication | JWT / JJWT 0.12.6 |
| Password hashing | BCrypt |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8.x |
| Migrations | Flyway |
| Rate limiting | Redis |
| Validation | Jakarta Bean Validation |
| API documentation | Springdoc OpenAPI |
| Monitoring | Spring Boot Actuator |
| Build | Maven |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, integration tests |

---

## 3. Architecture

```text
Client / Frontend
        |
        | REST + JWT
        v
Spring Security / Filters
        |
        v
Controllers
        |
        v
Domain Services
  |      |       |       |       |
 Auth   Wallet  Payout  Fraud  Reconciliation
        |         |
        |      Withdrawal
        |      /     \
        | Idempotency Audit
        |
        v
Repositories / JPA
        |
        v
      MySQL
        ^
      Flyway

Redis -> distributed rate limiting
Actuator -> health and metrics
Structured logs -> request/correlation tracing
```

Complete architecture:

```text
docs/architecture.md
```

---

## 4. Backend Source of Truth

The backend controls:

```text
Wallet balances
Required VES
Payout values
Payout availability
Withdrawal eligibility
Withdrawal status
Financial transactions
Ownership
Fraud decisions
Audit records
Reconciliation state
```

A client cannot safely change a wallet by modifying browser state or sending an arbitrary financial amount.

Example:

```text
Frontend attempts:
VES = 100000

Backend:
Uses database wallet balance and server-side business rules.
```

---

## 5. Wallet & Ledger

The wallet supports:

```text
VES
SVES
GEMS
TOKENS
SPINS
```

Every successful wallet mutation creates a ledger transaction containing fields such as:

```text
transaction_id
user_id
wallet_id
currency
transaction_type
amount
balance_before
balance_after
source
reference_id
status
description
metadata
created_at
```

Wallet accounting is documented in:

```text
docs/wallet-accounting.md
```

---

## 6. Payout Configuration

Payout configuration is controlled by the backend/database and is not hardcoded in the frontend.

Configured methods include:

| Method | State |
|---|---|
| UPI | Active |
| Amazon Gift Card | Active |
| Google Play Gift Card | Active |
| PayPal | Inactive until provider integration is enabled |

Configured INR payout values:

| Payout | Required VES |
|---:|---:|
| ₹10 | 2,400 |
| ₹25 | 5,800 |
| ₹50 | 10,000 |
| ₹100 | 19,500 |
| ₹150 | 28,500 |
| ₹300 | 52,500 |
| ₹500 | 80,500 |
| ₹1,000 | 150,000 |

The client sends a payout option ID. The backend resolves the actual financial values from stored configuration.

---

## 7. Withdrawal Flow

```text
Login
  ↓
JWT authentication
  ↓
Get payout configuration
  ↓
Select payout method
  ↓
Select payout option
  ↓
Enter payout details
  ↓
POST /api/withdrawals
  ↓
Idempotency validation
  ↓
Eligibility validation
  ↓
Payout validation
  ↓
Fraud/risk evaluation
  ↓
Resolve required VES
  ↓
Wallet debit for normal ALLOW path
  ↓
Ledger transaction
  ↓
PENDING withdrawal
  ↓
Admin processing / approval / rejection
```

Withdrawal statuses:

```text
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
```

For normally debited withdrawals, rejection/cancellation uses a compensating `CORRECTION` ledger transaction rather than deleting the original debit.

See:

```text
docs/withdrawal-flow.md
```

---

## 8. Idempotency

Withdrawal creation requires a valid idempotency key at the service layer.

```http
Idempotency-Key: <unique-value>
```

The backend creates a request fingerprint and stores the user/key combination in `withdrawal_idempotency`.

```text
Same key + same request
        ↓
Existing withdrawal returned
        ↓
No second financial operation
```

```text
Same key + different request
        ↓
Rejected
```

See:

```text
docs/idempotency.md
```

---

## 9. Fraud & Abuse Protection

The backend implements seven fraud/risk rules:

```text
Rapid Withdrawal
Repeated Withdrawal
Repeated Failed Request
Unusual Wallet Activity
Suspicious Payout Request
Suspicious Account Activity
Multiple Suspicious Payout Pattern
```

Risk evaluation supports:

```text
ALLOW
REVIEW
BLOCK
```

The scoring model uses configuration-driven rule parameters, normalized severity, nonlinear scoring, and configurable global thresholds.

The fraud-event table is an audit/result store; authoritative historical financial/activity data comes from the underlying wallet, withdrawal, and authentication records.

---

## 10. Rate Limiting

Redis-backed distributed rate limiting protects sensitive operations.

| Category | Limit | Window |
|---|---:|---:|
| Login | 5 | 60 seconds |
| Withdrawal creation | 5 | 60 seconds |
| Wallet mutations | 20 | 60 seconds |
| Withdrawal mutations | 20 | 60 seconds |

Exceeded limits return:

```text
429 Too Many Requests
```

See:

```text
docs/rate-limiting.md
```

---

## 11. Security

Security controls include:

```text
JWT authentication
Role-based authorization
Resource ownership checks
BCrypt password hashing
Server-side validation
Redis rate limiting
Withdrawal idempotency
Wallet optimistic locking
Database uniqueness constraints
Fraud/risk controls
Audit logging
Controlled error responses
Externalized secrets
Configuration-driven CORS
```

See:

```text
docs/security.md
```

---

## 12. Reconciliation

The backend provides an admin reconciliation endpoint:

```http
GET /api/admin/reconciliation/wallet/{walletId}?currency=VES
```

It compares:

```text
Stored wallet balance
        vs
Ledger-derived balance
```

A mismatch is recorded and audited rather than silently overwriting the wallet.

See:

```text
docs/reconciliation.md
```

---

## 13. API Surface

### Authentication

```http
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

### Wallet

```http
GET  /api/wallet
GET  /api/wallet/transactions
GET  /api/wallet/summary
POST /api/wallet/credit
POST /api/wallet/debit
```

### Payout Configuration

```http
GET /api/payouts/configuration
```

### Withdrawals

```http
POST  /api/withdrawals
GET   /api/withdrawals
GET   /api/withdrawals/{withdrawalId}
PATCH /api/withdrawals/{withdrawalId}/processing
PATCH /api/withdrawals/{withdrawalId}/approve
PATCH /api/withdrawals/{withdrawalId}/reject
PATCH /api/withdrawals/{withdrawalId}/cancel
```

### Reconciliation

```http
GET /api/admin/reconciliation/wallet/{walletId}
```

### Operational APIs

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/metrics
GET /v3/api-docs
GET /swagger-ui.html
```

Complete endpoint documentation:

```text
API_DOCUMENTATION.md
```

---

## 14. Database

Flyway controls schema evolution.

Current migration sequence:

```text
V1__create_users.sql
V2__create_wallets.sql
V3__create_wallet_transactions.sql
V4__create_payout_configuration.sql
V5__create_withdrawals.sql
V6__create_withdrawal_idempotency.sql
V7__create_withdrawal_audit.sql
V8__create_audit_log.sql
V9__create_fraud_risk_events.sql
V10__create_authentication_attempts.sql
V11__add_explanation_to_fraud_risk_events.sql
V12__create_payout_jobs.sql
```

See:

```text
docs/database.md
```

---

## 15. Payout Processing Architecture

The backend includes a persistent payout job model and worker/provider abstraction:

```text
Withdrawal
    ↓
PayoutJob
    ↓
PayoutWorker
    ↓
PayoutProviderRegistry
    ↓
PayoutProvider
```

Payout jobs use persisted status, attempt count, retry timing, and last-error information.

The project does **not** claim a production payout-provider integration. PayPal remains inactive until an appropriate provider is available.

---

## 16. Demo Data

Demo data can be enabled through:

```properties
DEMO_DATA_ENABLED=true
```

Demo account:

```text
Email:    demo@veloop.test
Password: Demo@12345
```

Demo wallet:

```text
VES    = 25,000
SVES   = 5,000
GEMS   = 100
TOKENS = 500
SPINS  = 3
```

The initializer creates sample ledger activity including:

```text
Ad Reward
Daily Reward
Referral Reward
Bonus
Withdrawal
SVES reward
Gems reward
Tokens reward
Spins bonus
```

Demo values are for development/testing only.

---

## 17. Local Setup

### Prerequisites

Install:

```text
Java 21
Maven or Maven Wrapper
MySQL 8.x
Redis
```

### Database

Create a development database:

```sql
CREATE DATABASE veloop_rewards;
```

### Environment

Copy:

```text
.env.example
```

to:

```text
.env
```

Fill in local values for:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
CORS_ALLOWED_ORIGINS
REDIS_HOST
REDIS_PORT
REDIS_TIMEOUT
DEMO_DATA_ENABLED
```

Never commit `.env`.

### Run

Windows:

```cmd
mvnw.cmd spring-boot:run
```

Other systems:

```bash
./mvnw spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

---

## 18. Testing

Run the full regression suite:

Windows:

```cmd
mvnw.cmd clean test
```

Other systems:

```bash
./mvnw clean test
```

### Current verified baseline

```text
257 / 257 tests passed
Failures: 0
Errors: 0
```

The test suite covers wallet accounting, concurrency, ownership, withdrawal lifecycle, payout validation, idempotency, fraud/risk, rate limiting, audit, payout processing, and reconciliation.

See:

```text
docs/testing.md
```

---

## 19. Postman

The repository contains:

```text
postman/VELOop_Rewards_Backend.postman_collection.json
```

Import the collection into Postman and start with:

```text
Login - Demo User
Current User
Get Wallet
Get Wallet Summary
Get Wallet Transactions
Get Payout Configuration
```

Then exercise the withdrawal and reconciliation flow.

---

## 20. Documentation

```text
docs/
├── architecture.md
├── database.md
├── wallet-accounting.md
├── withdrawal-flow.md
├── security.md
├── idempotency.md
├── rate-limiting.md
├── reconciliation.md
└── testing.md
```

Additional API documentation:

```text
API_DOCUMENTATION.md
```

---

## 21. Project Structure

```text
veloop-rewards-backend/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       └── db/migration/
│   └── test/
│
├── docs/
├── postman/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── README.md
├── API_DOCUMENTATION.md
├── .env.example
└── .gitignore
```

The application source is organized into modules/packages for:

```text
auth
security
user
wallet
payout
withdrawal
idempotency
fraud
payoutprocessing
reconciliation
audit
observability
config
common
```

---

## 22. Git & Security Rules

Commit:

```text
README.md
API_DOCUMENTATION.md
.env.example
docs/
postman/
src/
pom.xml
```

Never commit:

```text
.env
real credentials
JWT secrets
production database URLs
API keys
private keys
```

The existing `.gitignore` excludes `.env` and preserves `.env.example`.

---

## 23. Current Backend Requirement Coverage

| Requirement | Status |
|---|---|
| Backend-driven wallet | ✅ Complete |
| VES/SVES/Gems/Tokens/Spins | ✅ Complete |
| Wallet ledger | ✅ Complete |
| Credits/debits | ✅ Complete |
| Balance validation | ✅ Complete |
| Atomic wallet operations | ✅ Complete |
| Optimistic locking | ✅ Complete |
| Wallet APIs | ✅ Complete |
| Transaction history/pagination | ✅ Complete |
| Wallet summary | ✅ Complete |
| Payout configuration | ✅ Complete |
| Backend payout-value resolution | ✅ Complete |
| Withdrawal lifecycle | ✅ Complete |
| Wallet deduction/reversal | ✅ Complete |
| Ownership isolation | ✅ Complete |
| JWT authentication | ✅ Complete |
| Authorization | ✅ Complete |
| Idempotency | ✅ Complete |
| Withdrawal concurrency protection | ✅ Complete |
| Fraud & abuse protection | ✅ Complete |
| Redis rate limiting | ✅ Complete |
| Audit logging | ✅ Complete |
| Reconciliation | ✅ Complete |
| Payout job/worker architecture | ✅ Complete |
| Demo/seed data | ✅ Complete |
| Postman collection | ✅ Complete |
| API documentation | ✅ Complete |
| Architecture/database/security docs | ✅ Complete |
| Automated regression | ✅ 257/257 PASS |
| Manual API verification | ✅ Complete |

---

## 24. Remaining Overall Submission Work

The backend implementation and verification are complete.

The original internship submission also expects demonstration-level deliverables outside the backend itself:

```text
React demonstration frontend
Live frontend link
Live backend/API link
Short demonstration video
Final submission packaging
```

These are submission/deployment deliverables, not unfinished wallet accounting or backend-security defects.

---

## 25. Scalability Discussion

If the system grows from 1,000 to 1,000,000 users, the architecture should scale around the same financial invariants:

```text
Database transactions
Atomic wallet updates
Ledger-based accounting
Database indexing
Idempotency
Queue-based payout processing
Redis/shared caching where appropriate
Distributed rate limiting
Fraud detection
Audit logging
Reconciliation
Monitoring
Horizontal application scaling
```

The most important rule remains:

> Scale the infrastructure without weakening wallet correctness or financial consistency.

---

## 26. Final Backend Checkpoint

```text
BACKEND-FINAL-CHECKPOINT

Automated tests:         257/257 PASS
Manual API verification: COMPLETED
Documentation:           COMPLETED
Postman collection:      COMPLETED
Demo data:               COMPLETED

Backend status:
READY FOR FRONTEND / DEPLOYMENT / DEMONSTRATION
```