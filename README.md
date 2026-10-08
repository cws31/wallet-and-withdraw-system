# VELoop Rewards Wallet System

A production-oriented **Rewards Wallet and Withdrawal Backend** built with Spring Boot. The system manages user wallets, multi-currency reward balances, wallet ledger transactions, withdrawals, payout configuration, fraud and abuse detection, idempotency, rate limiting, payout processing, audit logging, observability, and financial reconciliation.

The backend is designed around the principle that the **server and database are the source of truth for all wallet and withdrawal operations**.

---

## 1. Project Overview

### VELoop Rewards Wallet System

VELoop is a backend system for managing reward balances and converting those rewards into configured payout options.

The system provides:

- User registration and authentication
- JWT-based authentication
- Role-based authorization
- Automatic wallet creation for new users
- Multi-currency reward wallets
- Wallet credit and debit operations
- Immutable-style wallet transaction ledger
- Withdrawal creation and lifecycle management
- Configurable payout methods and payout options
- Withdrawal idempotency
- Fraud and abuse risk detection
- Dynamic fraud risk scoring
- Withdrawal audit history
- General audit logging
- Database-backed payout queue
- Payout provider abstraction
- Payout retry handling
- Distributed rate limiting using Redis
- Correlation IDs and application metrics
- Financial reconciliation
- Swagger/OpenAPI documentation
- Database versioning using Flyway

---

# 2. Architecture

The application follows a layered Spring Boot architecture.

```text
Client
  |
  v
Spring Security
  |
  +---- JWT Authentication
  |
  +---- Rate Limiting
  |
  v
REST Controllers
  |
  v
Service Layer
  |
  +---- Authentication
  +---- Wallet
  +---- Withdrawals
  +---- Payout Configuration
  +---- Fraud Detection
  +---- Idempotency
  +---- Payout Processing
  +---- Reconciliation
  +---- Audit
  |
  v
Spring Data JPA
  |
  v
MySQL
  |
  +---- Wallets
  +---- Wallet Transactions
  +---- Withdrawals
  +---- Payout Configuration
  +---- Audit Records
  +---- Fraud Risk Events
  +---- Payout Jobs
  |
  +----------------------+
                         |
                         v
                       Redis
                  Rate Limiting
```

Detailed architecture documentation is available in:

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

---

# 3. Technology Stack

The technologies below are the technologies actually used by the project.

| Technology | Purpose |
|---|---|
| Java 21 | Application language |
| Spring Boot 3.5.16 | Backend framework |
| Spring Web | REST APIs |
| Spring Data JPA | Persistence layer |
| Hibernate | ORM |
| Spring Security | Authentication and authorization |
| JJWT 0.12.6 | JWT creation and validation |
| BCrypt | Password hashing |
| MySQL | Primary relational database |
| Flyway | Database migrations |
| Redis | Distributed rate limiting |
| Spring Boot Actuator | Health and metrics |
| SpringDoc OpenAPI 2.9.1 | Swagger/OpenAPI documentation |
| Lombok | Boilerplate reduction |
| Maven | Build and dependency management |
| JUnit / Spring Boot Test | Automated testing |

---

# 4. Project Structure

```text
src/
├── main/
│   ├── java/com/veloop/rewards/
│   │
│   ├── auth/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   │
│   ├── audit/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   │
│   ├── common/
│   │   ├── exception/
│   │   └── response/
│   │
│   ├── config/
│   │
│   ├── fraud/
│   │   ├── config/
│   │   ├── entity/
│   │   ├── enums/
│   │   ├── exception/
│   │   ├── repository/
│   │   ├── rule/
│   │   └── service/
│   │
│   ├── idempotency/
│   │
│   ├── observability/
│   │
│   ├── payout/
│   │
│   ├── payoutprocessing/
│   │
│   ├── reconciliation/
│   │
│   ├── security/
│   │
│   ├── user/
│   │
│   ├── wallet/
│   │
│   └── withdrawal/
│
└── main/resources/
    ├── application.properties
    └── db/
        └── migration/
            ├── V1__create_users.sql
            ├── V2__create_wallets.sql
            ├── V3__create_wallet_transactions.sql
            ├── V4__create_payout_configuration.sql
            ├── V5__create_withdrawals.sql
            ├── V6__create_withdrawal_idempotency.sql
            ├── V7__create_withdrawal_audit.sql
            ├── V8__create_audit_log.sql
            ├── V9__create_fraud_risk_events.sql
            ├── V10__create_authentication_attempts.sql
            ├── V11__add_explanation_to_fraud_risk_events.sql
            └── V12__create_payout_jobs.sql
```

---

# 5. Prerequisites

Install:

- Java 21
- MySQL 8.x
- Redis 6.x/7.x
- Git

Verify:

```bash
java -version
git --version
```

The project uses Java 21.

---

# 6. Setup

## Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd veloop-rewards-backend
```

## Configure environment variables

Create a `.env` file from `.env.example`.

```bash
cp .env.example .env
```

Configure the database, JWT, CORS, Redis, and other application settings.

Example structure:

```env
SERVER_PORT=8080

DB_URL=jdbc:mysql://localhost:3306/veloop_rewards
DB_USERNAME=root
DB_PASSWORD=your_password

JWT_SECRET=your_base64_encoded_secret
JWT_EXPIRATION=900000

CORS_ALLOWED_ORIGINS=http://localhost:3000

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_TIMEOUT=2000ms
```

Do not commit real secrets.

---

# 7. Database Setup

Create the MySQL database:

```sql
CREATE DATABASE veloop_rewards;
```

Configure:

```env
DB_URL=jdbc:mysql://localhost:3306/veloop_rewards
DB_USERNAME=root
DB_PASSWORD=your_password
```

The application uses:

```properties
spring.jpa.hibernate.ddl-auto=none
```

Database schema creation and changes are managed by **Flyway migrations**.

On application startup, Flyway applies migrations:

```text
V1 → V2 → V3 → ... → V12
```

The migrations create:

- users
- wallets
- wallet_transactions
- payout_methods
- payout_options
- withdrawals
- withdrawal_idempotency
- withdrawal_audit
- audit_log
- fraud_risk_events
- authentication_attempts
- payout_jobs

---

# 8. Redis Setup

Redis is used for distributed rate limiting.

Start Redis locally:

```bash
redis-server
```

Default configuration:

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_TIMEOUT=2000ms
```

The application uses Redis-backed rate limiting rather than keeping rate-limit state only inside one application instance.

---

# 9. Run the Application

Using the Maven wrapper:

### Windows

```bash
mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

If the wrapper does not have execute permission:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

The default server port is:

```text
8080
```

Application:

```text
http://localhost:8080
```

---

# 10. API Documentation

The project uses SpringDoc OpenAPI.

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

---

# 11. Authentication APIs

Base URL:

```text
/api/auth
```

## Register

```http
POST /api/auth/register
```

Creates a new user and automatically creates the user's wallet.

Example:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "Password@123"
}
```

---

## Login

```http
POST /api/auth/login
```

Authenticates the user and returns a signed JWT.

Example:

```json
{
  "email": "john@example.com",
  "password": "Password@123"
}
```

The returned token must be sent with protected requests:

```http
Authorization: Bearer <JWT>
```

---

## Current User

```http
GET /api/auth/me
```

Requires authentication.

---

# 12. Wallet APIs

Base URL:

```text
/api/wallet
```

## Get Wallet

```http
GET /api/wallet
```

Returns the authenticated user's wallet.

---

## Get Wallet Summary

```http
GET /api/wallet/summary
```

Returns wallet balances and transaction summary information.

---

## Get Transactions

```http
GET /api/wallet/transactions?page=1&limit=20
```

Returns paginated wallet transaction history.

The maximum page size is limited to 100.

---

## Credit Wallet

```http
POST /api/wallet/credit
```

**ADMIN only.**

Used to credit a user's wallet.

The wallet service validates:

- currency
- amount
- transaction type
- balance transition
- transaction creation
- audit logging

---

## Debit Wallet

```http
POST /api/wallet/debit
```

**ADMIN only.**

Used to debit a user's wallet.

The service checks available balance before performing the debit.

If the balance is insufficient, the operation is rejected.

---

# 13. Supported Wallet Currencies

The wallet model supports multiple reward currencies:

```text
VES
SVES
GEMS
TOKENS
SPINS
```

The wallet stores an independent balance for each supported currency.

For example:

```text
VES      → reward balance
SVES     → secondary reward balance
GEMS     → gems
TOKENS   → tokens
SPINS    → spins
```

---

# 14. Wallet Accounting

Every wallet mutation creates a wallet transaction.

A transaction records:

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

Example:

```text
Balance Before: 1000 VES
Debit:           250 VES
Balance After:   750 VES
```

The transaction history therefore provides a ledger trail for wallet movements.

---

# 15. Withdrawal APIs

Base URL:

```text
/api/withdrawals
```

## Create Withdrawal

```http
POST /api/withdrawals
```

Requires:

```http
Authorization: Bearer <JWT>
Idempotency-Key: <unique-key>
```

The client provides the selected payout method and payout option.

The backend resolves the actual payout configuration and required wallet amount.

The client cannot directly determine or override the wallet deduction amount.

---

## Get Withdrawal History

```http
GET /api/withdrawals?page=1&limit=20
```

Returns the authenticated user's withdrawal history.

---

## Get Withdrawal Details

```http
GET /api/withdrawals/{withdrawalId}
```

Returns a withdrawal belonging to the authenticated user.

---

## Mark Withdrawal as Processing

```http
PATCH /api/withdrawals/{withdrawalId}/processing
```

**ADMIN only.**

Moves an eligible withdrawal into:

```text
PROCESSING
```

---

## Approve Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/approve
```

**ADMIN only.**

Approves an eligible withdrawal.

---

## Reject Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/reject
```

**ADMIN only.**

Requires a rejection reason.

Example:

```text
PATCH /api/withdrawals/{withdrawalId}/reject?rejectionReason=Invalid payout details
```

When a withdrawal that already deducted VES is rejected, the system reverses the deducted VES through a wallet correction transaction.

---

## Cancel Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
```

**USER only.**

Cancels an eligible withdrawal and reverses the previously deducted VES.

---

# 16. Withdrawal Lifecycle

A withdrawal is managed through controlled state transitions.

Typical flow:

```text
PENDING
   |
   +----> CANCELLED
   |
   +----> REJECTED
   |
   v
PROCESSING
   |
   +----> REJECTED
   |
   v
APPROVED
```

Payout processing is handled separately through the payout job mechanism.

---

# 17. Payout Configuration

Base URL:

```text
/api/payouts
```

## Get Payout Configuration

```http
GET /api/payouts/configuration
```

Requires authentication.

The API returns active payout methods and active payout options.

The current database configuration includes payout methods such as:

```text
UPI
Amazon Gift Card
Google Play Gift Card
PayPal
```

Only configured active options are exposed for selection.

Payout validation is handled by:

```text
PayoutOptionValidator
PayoutDetailValidator
```

This keeps payout configuration and validation on the backend rather than trusting client-provided payout values.

---

# 18. Payout Processing

Approved withdrawals can be placed into a database-backed payout queue.

Main components:

```text
PayoutQueue
DatabasePayoutQueue
PayoutJob
PayoutWorker
PayoutProvider
PayoutProviderRegistry
PayoutRetryPolicy
```

The system supports a provider abstraction so payout execution is separated from withdrawal creation.

The payout job stores:

```text
withdrawal
status
attempt count
next attempt time
last error
```

The worker handles provider execution and retry/failure behavior.

---

# 19. Database Architecture

The project uses **MySQL + Spring Data JPA + Flyway**.

## Main Tables

```text
users
   |
   +---- wallets
   |
   +---- wallet_transactions
   |
   +---- withdrawals
   |
   +---- withdrawal_idempotency
   |
   +---- withdrawal_audit
   |
   +---- audit_log
   |
   +---- fraud_risk_events
   |
   +---- authentication_attempts

payout_methods
   |
   +---- payout_options

withdrawals
   |
   +---- payout_jobs
```

---

# 20. Wallet

The `wallets` table represents the current wallet state.

It contains balances for:

```text
VES
SVES
GEMS
TOKENS
SPINS
```

Each user has one wallet.

A unique constraint prevents multiple wallets from being created for the same user.

---

# 21. Ledger / Wallet Transactions

The `wallet_transactions` table records wallet movements.

Important fields include:

```text
transaction_id
wallet_id
currency
transaction_type
amount
balance_before
balance_after
status
source
reference_id
created_at
```

This provides an auditable transaction history and allows the system to derive wallet balances from completed transactions.

---

# 22. Withdrawals

The `withdrawals` table stores:

```text
withdrawal_id
user_id
payout_method_id
payout_option_id
currency
currency_amount
payout_amount
payout_details
status
rejection_reason
review_note
transaction_id
requested_at
processed_at
created_at
updated_at
```

A withdrawal is linked to the payout configuration and, where applicable, the wallet transaction that deducted the user's reward balance.

---

# 23. Payout

Payout configuration is separated into:

```text
payout_methods
payout_options
```

A method represents the payout channel.

An option represents a configured payout amount/value.

Example:

```text
UPI
 ├── Option 1
 ├── Option 2
 ├── Option 3
 └── ...
```

This allows payout values to be managed through database configuration instead of hardcoding them into withdrawal requests.

---

# 24. Audit

The project maintains multiple audit mechanisms.

## General Audit Log

```text
audit_log
```

Records actions such as wallet operations and reconciliation failures.

It can store:

```text
actor
target user
target type
action
reference ID
metadata
timestamp
```

## Withdrawal Audit

```text
withdrawal_audit
```

Tracks withdrawal state transitions.

It records:

```text
old status
new status
action
performed by
description
timestamp
```

This makes important withdrawal lifecycle changes traceable.

---

# 25. Fraud & Abuse Protection

The withdrawal flow includes a configurable fraud risk engine.

The fraud system evaluates multiple independent rules.

Implemented rules include:

```text
1. Rapid Withdrawal
2. Repeated Withdrawal
3. Repeated Failed Request
4. Unusual Wallet Activity
5. Suspicious Payout Request
6. Suspicious Account Activity
7. Multiple Suspicious Payout Pattern
```

The rules use authoritative wallet, withdrawal, authentication, and payout data rather than using previously generated fraud events as the primary source of truth.

---

# 26. Dynamic Fraud Risk Scoring

Risk scoring is configurable through application properties.

Each rule has configuration for:

```text
baseline
time window
maximum expected activity
maximum rule score
```

The scoring model normalizes rule severity and applies nonlinear scoring.

Conceptually:

```text
severity = normalized activity level

ruleScore =
    severity² × maxRuleScore
```

The overall fraud engine evaluates the triggered rules and produces a risk decision.

Risk decisions can result in outcomes such as:

```text
ALLOW
REVIEW
BLOCK
```

Global review and block thresholds are configurable.

Fraud events are persisted in:

```text
fraud_risk_events
```

The event records:

```text
user
withdrawal
risk score
decision
review status
triggered rules
explanation
review information
timestamps
```

---

# 27. Authentication Security

The application uses Spring Security with stateless JWT authentication.

## Password Security

Passwords are never stored in plain text.

Passwords are hashed using:

```text
BCryptPasswordEncoder
```

## JWT

The login API returns a signed JWT.

The token contains information including:

```text
user ID
email
role
issued time
expiration time
```

The JWT expiration is configurable.

Default:

```text
900000 ms
```

which is 15 minutes.

---

# 28. Authorization

The application uses role-based authorization.

Roles include:

```text
USER
ADMIN
```

Examples:

### Public

```text
POST /api/auth/register
POST /api/auth/login
GET  /swagger-ui/**
GET  /v3/api-docs/**
GET  /actuator/health/**
```

### Authenticated users

```text
GET /api/auth/me
GET /api/wallet
GET /api/wallet/summary
GET /api/wallet/transactions
GET /api/withdrawals
POST /api/withdrawals
GET /api/withdrawals/{id}
GET /api/payouts/configuration
```

### ADMIN

```text
POST /api/wallet/credit
POST /api/wallet/debit

PATCH /api/withdrawals/{id}/processing
PATCH /api/withdrawals/{id}/approve
PATCH /api/withdrawals/{id}/reject

GET /api/admin/reconciliation/wallet/{walletId}
```

### USER

```text
PATCH /api/withdrawals/{id}/cancel
```

Authorization is enforced using Spring Security and method-level `@PreAuthorize`.

---

# 29. Validation

Request validation uses Jakarta Bean Validation.

Examples include:

```text
@NotNull
@NotBlank
@Positive
@Size
```

The application also performs service-level business validation.

Examples:

- Amount must be positive
- Currency must be valid
- Wallet must exist
- Payout method must exist
- Payout option must be active
- Payout option must belong to the selected payout method
- Withdrawal must be in a valid state
- Wallet must have sufficient balance
- Required Idempotency-Key must be supplied
- User must own the requested withdrawal

---

# 30. Atomic Wallet Operations

Wallet mutations are performed inside transactional service operations.

A wallet debit follows the general sequence:

```text
Request
  |
  v
Validate amount
  |
  v
Load wallet
  |
  v
Check balance
  |
  v
Calculate new balance
  |
  v
Update wallet
  |
  v
Create ledger transaction
  |
  v
Create audit record
  |
  v
Commit transaction
```

If an operation fails, the transaction is rolled back so the wallet and its ledger entry are not left in an inconsistent state.

The project also contains concurrency and atomicity integration tests for wallet operations.

---

# 31. Insufficient Balance Protection

Debit operations validate the current balance before subtraction.

Example:

```text
Wallet balance = 500 VES
Requested debit = 700 VES
```

Result:

```text
Rejected
```

The wallet must never become negative because of a normal debit operation.

The application raises an `InsufficientBalanceException`.

---

# 32. Idempotency

Withdrawal creation requires:

```http
Idempotency-Key: <unique-key>
```

The key is stored together with a request fingerprint.

Database constraint:

```text
UNIQUE(user_id, idempotency_key)
```

This prevents accidental duplicate withdrawal creation caused by:

- double-clicking
- network retries
- client retries
- duplicated requests
- timeout recovery

The system also detects reuse of the same idempotency key with a different request payload.

---

# 33. Rate Limiting

The application implements rate limiting using Redis.

Configured limits include:

### Login / Registration

```text
5 requests / 60 seconds
```

### Withdrawal creation

```text
5 requests / 60 seconds
```

### Wallet mutations

```text
20 requests / 60 seconds
```

### Withdrawal mutations

```text
20 requests / 60 seconds
```

When the limit is exceeded, the API returns:

```http
429 Too Many Requests
```

and provides a `Retry-After` header.

Rate limiting can be configured through:

```properties
security.rate-limit.enabled=true
```

---

# 34. Financial Reconciliation

The application includes a reconciliation service for detecting wallet/ledger inconsistencies.

Endpoint:

```http
GET /api/admin/reconciliation/wallet/{walletId}?currency=VES
```

**ADMIN only.**

The service compares:

```text
Current Wallet Balance
        vs
Ledger-Derived Balance
```

Conceptually:

```text
difference =
    walletBalance - ledgerDerivedBalance
```

If:

```text
difference == 0
```

the wallet is reconciled.

Otherwise the system:

- reports the mismatch
- records a reconciliation failure audit event
- records an observability metric

---

# 35. Observability

The application uses Spring Boot Actuator and custom metrics.

Health endpoint:

```text
GET /actuator/health
```

Metrics endpoint:

```text
GET /actuator/metrics
```

The application also uses correlation IDs through:

```text
CorrelationIdFilter
```

Structured logging is configured using Logstash-compatible JSON formatting.

Metrics cover important operations such as:

- wallet credit success/failure
- wallet debit success/failure
- reconciliation success/failure
- other operational events

---

# 36. API Response Format

The application uses common response wrappers.

Successful APIs generally return an `ApiResponse`.

Example structure:

```json
{
  "success": true,
  "message": "Wallet retrieved successfully",
  "data": {}
}
```

Errors are handled centrally through:

```text
GlobalExceptionHandler
```

Security-related errors are handled by dedicated security exception handlers.

---

# 37. Testing

The project contains unit tests and integration tests across the major business areas.

The test suite covers:

```text
Authentication
Wallet
Wallet transactions
Wallet consistency
Wallet isolation
Wallet concurrency
Wallet atomicity
Withdrawals
Withdrawal concurrency
Withdrawal eligibility
Withdrawal fraud decisions
Withdrawal idempotency
Payout configuration
Payout validation
Payout processing
Payout queue
Payout provider
Payout worker
Fraud rules
Fraud risk scoring
Rate limiting
Observability
Reconciliation
Audit behavior
```

The repository currently contains approximately **51 test classes** under:

```text
src/test/java/
```

---

# 38. Evaluator Testing Guide

An evaluator can test the main wallet and withdrawal functionality using Swagger UI, Postman, curl, or another REST client.

---

## Step 1 — Register a User

```http
POST /api/auth/register
```

Example:

```json
{
  "name": "Test User",
  "email": "test@example.com",
  "password": "Password@123"
}
```

A wallet is automatically created for the user.

---

## Step 2 — Login

```http
POST /api/auth/login
```

Example:

```json
{
  "email": "test@example.com",
  "password": "Password@123"
}
```

Copy the returned JWT.

Use:

```http
Authorization: Bearer <JWT>
```

for protected requests.

---

## Step 3 — Check Wallet

```http
GET /api/wallet
```

Verify the wallet exists and inspect the supported balances.

---

## Step 4 — Check Payout Configuration

```http
GET /api/payouts/configuration
```

Use the returned payout method and payout option IDs for withdrawal testing.

---

## Step 5 — Credit Wallet

Login as an `ADMIN` user and call:

```http
POST /api/wallet/credit
```

Verify:

```text
wallet balance increases
wallet transaction is created
audit record is created
```

---

## Step 6 — Debit Wallet

As an `ADMIN`, call:

```http
POST /api/wallet/debit
```

Verify:

```text
wallet balance decreases
ledger transaction is created
```

---

## Step 7 — Test Insufficient Balance

Attempt to debit more than the current balance.

Example:

```text
Balance = 100 VES
Debit = 1000 VES
```

Expected result:

```text
Request rejected
InsufficientBalanceException
Wallet remains unchanged
No successful debit ledger entry
```

---

## Step 8 — Create Withdrawal

Use an authenticated user.

Send:

```http
POST /api/withdrawals
```

with:

```http
Authorization: Bearer <JWT>
Idempotency-Key: withdrawal-test-001
```

Use a valid payout method and payout option from:

```text
GET /api/payouts/configuration
```

Expected behavior:

```text
Withdrawal created
Required VES calculated by backend
Wallet balance deducted
Wallet transaction recorded
Withdrawal audit recorded
Fraud risk evaluation performed
```

---

## Step 9 — Test Duplicate Withdrawal Request

Send the exact same request again with:

```http
Idempotency-Key: withdrawal-test-001
```

Expected behavior:

```text
No second withdrawal should be created.
```

The idempotency mechanism protects the operation from duplicate requests.

---

## Step 10 — Test Idempotency Conflict

Reuse:

```text
withdrawal-test-001
```

but change the request payload.

Expected behavior:

```text
Idempotency conflict
```

The same key must not be used to represent a different withdrawal request.

---

## Step 11 — Test Withdrawal History

```http
GET /api/withdrawals
```

Verify the created withdrawal appears in the authenticated user's history.

---

## Step 12 — Test Withdrawal Cancellation

As the withdrawal owner:

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
```

Expected behavior:

```text
Withdrawal becomes CANCELLED
Previously deducted VES is reversed
A correction transaction is created
Audit information is recorded
```

---

## Step 13 — Test Admin Withdrawal Processing

As an `ADMIN`:

```http
PATCH /api/withdrawals/{withdrawalId}/processing
```

Then:

```http
PATCH /api/withdrawals/{withdrawalId}/approve
```

Verify the withdrawal state transitions.

---

## Step 14 — Test Withdrawal Rejection

As an `ADMIN`:

```http
PATCH /api/withdrawals/{withdrawalId}/reject?rejectionReason=Invalid payout details
```

Expected behavior:

```text
Withdrawal becomes REJECTED
Previously deducted VES is reversed
Correction ledger transaction is created
Withdrawal audit is recorded
```

---

## Step 15 — Test Rate Limiting

Repeatedly call a rate-limited endpoint within the configured window.

For example:

```text
POST /api/withdrawals
```

After exceeding the configured threshold, the application should return:

```http
429 Too Many Requests
```

with:

```http
Retry-After
```

---

## Step 16 — Test Reconciliation

As an `ADMIN`:

```http
GET /api/admin/reconciliation/wallet/{walletId}?currency=VES
```

The response reports:

```text
wallet balance
ledger-derived balance
difference
reconciled
```

A correctly maintained wallet should return:

```text
reconciled = true
```

---

# 39. Important Security Rules

The following principles are enforced by the backend:

```text
Client is not the source of truth
        ↓
Backend validates every financial operation
        ↓
Database persists wallet state
        ↓
Ledger records wallet movements
        ↓
Audit records important actions
        ↓
Idempotency prevents duplicate withdrawals
        ↓
Fraud engine evaluates suspicious activity
        ↓
Rate limiting reduces abuse
        ↓
Reconciliation detects financial inconsistencies
```

---

# 40. Configuration

Important configuration categories include:

```text
Database
JWT
CORS
Redis
Rate limiting
Fraud rules
Actuator
Metrics
Logging
Swagger/OpenAPI
```

Fraud rules are configurable using environment-backed properties such as:

```text
FRAUD_RAPID_WITHDRAWAL_WINDOW_MINUTES
FRAUD_RAPID_WITHDRAWAL_BASELINE
FRAUD_RAPID_WITHDRAWAL_MAX_EXPECTED
FRAUD_RAPID_WITHDRAWAL_MAX_SCORE
```

and equivalent properties for the other fraud rules.

This allows fraud sensitivity to be changed without modifying the rule implementation.

---

# 41. Database Migration Strategy

Flyway manages schema evolution.

Current migration sequence:

```text
V1  Users
V2  Wallets
V3  Wallet Transactions
V4  Payout Configuration
V5  Withdrawals
V6  Withdrawal Idempotency
V7  Withdrawal Audit
V8  Audit Log
V9  Fraud Risk Events
V10 Authentication Attempts
V11 Fraud Risk Event Explanation
V12 Payout Jobs
```

Hibernate does not automatically create or modify the production schema.

```properties
spring.jpa.hibernate.ddl-auto=none
```

---

# 42. Production-Oriented Design

The project is intentionally designed around financial consistency and backend ownership.

Important design principles include:

### Backend as source of truth

Wallet balances, payout values, withdrawal states, and financial calculations are controlled by the backend.

### Transactional consistency

Wallet updates, ledger entries, and important audit operations are performed transactionally.

### Concurrency protection

The wallet and withdrawal layers include concurrency-aware logic and integration tests.

### Idempotent financial requests

Withdrawal creation requires an idempotency key.

### Fraud protection

Suspicious activity is evaluated using configurable risk rules.

### Operational resilience

Payout processing uses a database-backed queue, provider abstraction, retries, and failure handling.

### Reconciliation

Wallet state can be compared against ledger-derived balances.

### Observability

Health checks, metrics, correlation IDs, and structured logging support operational diagnosis.

---

# 43. Documentation

Additional technical documentation is available under:

```text
docs/
```

### Architecture

```text
docs/architecture.md
```

### Database

```text
docs/database.md
```

### Wallet Accounting

```text
docs/wallet-accounting.md
```

### Withdrawal Flow

```text
docs/withdrawal-flow.md
```

### Security

```text
docs/security.md
```

### Idempotency

```text
docs/idempotency.md
```

### Rate Limiting

```text
docs/rate-limiting.md
```

### Reconciliation

```text
docs/reconciliation.md
```

### Testing

```text
docs/testing.md
```

---

# 44. Build

Compile the project:

```bash
./mvnw clean compile
```

Windows:

```bash
mvnw.cmd clean compile
```

Run tests:

```bash
./mvnw test
```

Windows:

```bash
mvnw.cmd test
```

Run the application:

```bash
./mvnw spring-boot:run
```

---

# 45. Quick Evaluation Checklist

An evaluator should be able to verify:

- [ ] Register user
- [ ] Wallet automatically created
- [ ] Login and receive JWT
- [ ] Access protected APIs
- [ ] Retrieve wallet
- [ ] Retrieve wallet summary
- [ ] Retrieve transaction history
- [ ] Retrieve payout configuration
- [ ] Credit wallet as admin
- [ ] Debit wallet as admin
- [ ] Reject insufficient balance
- [ ] Create withdrawal
- [ ] Require Idempotency-Key
- [ ] Prevent duplicate withdrawal
- [ ] Detect idempotency conflict
- [ ] View withdrawal history
- [ ] Cancel withdrawal
- [ ] Process withdrawal as admin
- [ ] Approve withdrawal as admin
- [ ] Reject withdrawal as admin
- [ ] Verify balance reversal
- [ ] Verify wallet ledger entries
- [ ] Verify audit entries
- [ ] Exercise fraud detection
- [ ] Exercise rate limiting
- [ ] Run reconciliation
- [ ] Access health endpoint
- [ ] Access Swagger/OpenAPI

---

# 46. Project Status

The project currently contains the implemented backend components for:

```text
Authentication
Wallet Management
Wallet Accounting
Withdrawals
Payout Configuration
Payout Processing
Fraud & Abuse Protection
Idempotency
Rate Limiting
Audit Logging
Observability
Financial Reconciliation
Automated Testing
Technical Documentation
```

The repository is structured as a backend-focused financial/rewards system rather than a simple CRUD wallet application.

---

## License

This project was developed as the VELoop Rewards Wallet backend implementation and evaluation project.