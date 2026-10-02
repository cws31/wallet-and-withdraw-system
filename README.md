# VELoop Rewards — Wallet & Withdrawal Backend

> **Development Status:** 🚧 **IN DEVELOPMENT**
> **Current Phase:** Backend Core System — Completed
> **Next Phase:** Backend Production Hardening & Requirement Completion
> **Target:** 100% Backend Requirement Completion → Frontend Development

---

## 📌 Project Overview

**VELoop Rewards Wallet & Withdrawal Backend** is a secure, backend-driven wallet and withdrawal system designed for the VELoop Rewards platform.

The system manages multiple internal reward currencies, wallet balances, transaction history, payout configurations, withdrawals, authentication, authorization, idempotency, rate limiting, audit logging, and concurrency-safe wallet operations.

The primary design principle is:

> **The backend is the single source of truth for all wallet, reward, withdrawal, and payout-related operations.**

Clients are never trusted to determine:

* Wallet balances
* Reward amounts
* Withdrawal amounts
* Required VEs
* Payout values
* Eligibility
* Transaction status
* Withdrawal status
* Payout configuration

All financially sensitive operations are validated and executed on the server.

---

# 🎯 Project Objective

The objective of this project is to build a complete and secure wallet backend capable of supporting:

* Multiple reward currencies
* Backend-controlled wallet balances
* Double-entry-style transaction history / ledger records
* Wallet credits and debits
* Withdrawal requests
* Payout configuration
* Payout validation
* Withdrawal lifecycle management
* Balance validation
* Atomic wallet operations
* Concurrent withdrawal protection
* Duplicate request protection
* Idempotency
* JWT authentication
* Role-based authorization
* User isolation
* Rate limiting
* Audit logging
* Database consistency
* API documentation
* Automated testing
* Future scalability toward large user volumes

---

# 🏗️ Current Development Status

| Area                             | Status        |
| -------------------------------- | ------------- |
| Spring Boot Backend              | ✅ Implemented |
| Authentication                   | ✅ Implemented |
| JWT Security                     | ✅ Implemented |
| User Management                  | ✅ Implemented |
| Wallet Management                | ✅ Implemented |
| Multi-Currency Wallet            | ✅ Implemented |
| Wallet Ledger                    | ✅ Implemented |
| Credit / Debit Operations        | ✅ Implemented |
| Balance Validation               | ✅ Implemented |
| Atomic Wallet Operations         | ✅ Implemented |
| Optimistic Locking               | ✅ Implemented |
| Payout Configuration             | ✅ Implemented |
| Withdrawal System                | ✅ Implemented |
| Withdrawal Lifecycle             | ✅ Implemented |
| Immediate Wallet Deduction       | ✅ Implemented |
| Withdrawal Reversal              | ✅ Implemented |
| Idempotency                      | ✅ Implemented |
| Duplicate Request Protection     | ✅ Implemented |
| User Isolation                   | ✅ Implemented |
| Rate Limiting                    | ✅ Implemented |
| Audit Logging                    | ✅ Implemented |
| Database Migrations              | ✅ Implemented |
| Database Indexing                | ✅ Implemented |
| Validation / Error Handling      | ✅ Implemented |
| Automated Tests                  | ✅ Implemented |
| Advanced Fraud Detection         | ⏳ Planned     |
| Financial Reconciliation         | ⏳ Planned     |
| Distributed Rate Limiting        | ⏳ Planned     |
| Queue-Based Payout Processing    | ⏳ Planned     |
| Production Monitoring            | ⏳ Planned     |
| Production Scalability Hardening | ⏳ Planned     |
| Frontend                         | ⏳ Planned     |

---

# 🚀 Current Backend Checkpoint

## `BACKEND-CHECKPOINT-01`

### Core Wallet & Withdrawal System

The current implementation contains the primary backend functionality required for the wallet and withdrawal system.

### Completed capabilities

* User registration and authentication
* JWT-based authentication
* Role-based authorization
* User wallet creation
* Multi-currency wallet
* Wallet balance APIs
* Wallet summary
* Transaction history
* Wallet credit operations
* Wallet debit operations
* Backend balance validation
* Atomic wallet mutations
* Optimistic locking
* Transaction ledger
* Payout methods
* Payout options
* Backend-controlled payout denominations
* Withdrawal creation
* Withdrawal status management
* Immediate wallet deduction
* Withdrawal cancellation
* Withdrawal rejection reversal
* Withdrawal approval
* Withdrawal processing
* Idempotency
* Rate limiting
* Audit logging
* Database migrations
* API validation
* Global exception handling
* Integration testing

---

# 💰 Supported Wallet Currencies

The wallet is designed to support multiple internal VELoop currencies.

| Currency | Purpose                              |
| -------- | ------------------------------------ |
| VES      | Primary withdrawal / reward currency |
| SVES     | Secondary reward currency            |
| GEMS     | Reward currency                      |
| TOKENS   | Reward currency                      |
| SPINS    | Reward/game currency                 |

All currency balances are maintained on the backend.

The client cannot directly modify these balances.

---

# 🧾 Wallet Ledger

Every wallet modification creates a corresponding transaction record.

The ledger records:

* Transaction ID
* User ID
* Wallet ID
* Currency
* Transaction type
* Amount
* Balance before
* Balance after
* Source
* Reference ID
* Status
* Description
* Metadata
* Creation timestamp

This provides a traceable history of wallet movements.

---

# 🔄 Transaction Types

### Credit Transactions

Supported credit types include:

* `REWARD`
* `BONUS`
* `REFERRAL`
* `DAILY_REWARD`
* `AD_REWARD`
* `GAME_REWARD`
* `ADMIN_CREDIT`
* `EXCHANGE_CREDIT`

### Debit Transactions

Supported debit types include:

* `WITHDRAWAL`
* `EXCHANGE_DEBIT`
* `ADMIN_DEBIT`
* `CORRECTION`

Transaction statuses include:

* `PENDING`
* `COMPLETED`
* `FAILED`
* `REVERSED`

---

# 💳 Wallet APIs

## Get Wallet

```http
GET /api/wallet
```

Returns the authenticated user's wallet balances.

---

## Get Wallet Summary

```http
GET /api/wallet/summary
```

Returns wallet-level summary information.

---

## Get Transaction History

```http
GET /api/wallet/transactions?page=1&limit=20
```

Transaction history is paginated and sorted by creation time.

The backend also restricts the maximum requested page size.

---

## Credit Wallet

```http
POST /api/wallet/credit
```

Used for protected/internal wallet credit operations.

---

## Debit Wallet

```http
POST /api/wallet/debit
```

Used for protected/internal wallet debit operations.

All wallet mutations are processed through backend services rather than directly manipulating database balances from controllers.

---

# 🔐 Authentication & Authorization

The backend uses:

* JWT authentication
* Spring Security
* BCrypt password hashing
* Stateless authentication
* Role-based authorization
* Method-level authorization
* User ownership validation

### Authentication APIs

```http
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

JWT tokens contain authenticated user information and role information.

---

# 🛡️ Security Architecture

Security-sensitive operations are protected using multiple layers.

### Implemented security controls

* JWT authentication
* Role-based access control
* User ownership checks
* Request validation
* Backend balance validation
* Payout option validation
* Withdrawal state validation
* Idempotency
* Rate limiting
* Optimistic locking
* Atomic database transactions
* Audit logging
* Global exception handling
* Password hashing

---

# 🏦 Payout Configuration

Payout options are controlled by the backend.

The frontend is not allowed to decide:

* Required VES
* Payout amount
* Currency
* Payout method
* Option availability
* Active/inactive state

---

## Current Payout Methods

| Method                | Status   |
| --------------------- | -------- |
| UPI                   | Active   |
| Amazon Gift Card      | Active   |
| Google Play Gift Card | Active   |
| PayPal                | Inactive |

---

# 💵 Current UPI Redemption Configuration

| Payout | Required VES |
| -----: | -----------: |
|    ₹10 |    2,400 VES |
|    ₹25 |    5,800 VES |
|    ₹50 |   10,000 VES |
|   ₹100 |   19,500 VES |
|   ₹150 |   28,500 VES |
|   ₹300 |   52,500 VES |
|   ₹500 |   80,500 VES |
| ₹1,000 |  150,000 VES |

These values are stored and resolved from backend configuration rather than trusted from the client.

---

# 💸 Withdrawal Architecture

The withdrawal process follows the backend-controlled flow:

```text
Authenticated User
        ↓
Withdrawal Request
        ↓
Validate Authentication
        ↓
Validate User Eligibility
        ↓
Resolve Payout Method
        ↓
Resolve Payout Option
        ↓
Resolve Required Currency Amount
        ↓
Validate Payout Details
        ↓
Check Wallet Balance
        ↓
Apply Idempotency Check
        ↓
Atomically Deduct VES
        ↓
Create Wallet Ledger Transaction
        ↓
Create Withdrawal
        ↓
Create Audit Record
        ↓
PENDING
```

The current implementation uses:

> **Immediate deduction strategy**

The required VES is deducted when the withdrawal request is successfully created.

---

# 🔁 Withdrawal Lifecycle

Supported withdrawal states:

```text
PENDING
   ↓
PROCESSING
   ↓
APPROVED
```

Alternative paths:

```text
PENDING → CANCELLED
```

or:

```text
PENDING / PROCESSING → REJECTED
```

When a withdrawal is rejected or cancelled, the deducted VES can be restored through a correction ledger transaction.

---

# 🧾 Withdrawal Data

A withdrawal stores:

* Withdrawal ID
* User ID
* Payout method
* Payout option
* Currency
* Currency amount
* Payout amount
* Payout details
* Status
* Rejection reason
* Review note
* Related transaction
* Requested timestamp
* Processed timestamp
* Updated timestamp

---

# 🔄 Idempotency

Withdrawal creation requires an:

```http
Idempotency-Key
```

The backend stores the idempotency key together with the request fingerprint.

This protects against:

* Double-click submissions
* Browser retries
* Network retries
* Duplicate API requests
* Repeated withdrawal submissions

The same authenticated user sending the same idempotency key and request can receive the existing withdrawal rather than creating another one.

A reused key with a different request is rejected.

---

# 🚦 Rate Limiting

Sensitive endpoints are protected using rate limiting.

The current implementation uses an:

> **In-memory fixed-window rate limiting approach**

Current limits include:

| Endpoint Category    |                Limit |
| -------------------- | -------------------: |
| Login                |  5 requests / 60 sec |
| Withdrawal Creation  |  5 requests / 60 sec |
| Wallet Mutations     | 20 requests / 60 sec |
| Withdrawal Mutations | 20 requests / 60 sec |

Rate limits are applied using IP-based or authenticated-user-based identification depending on the endpoint.

When the limit is exceeded:

```http
HTTP 429 TOO MANY REQUESTS
```

The response also provides retry information.

### Current limitation

The current rate limiter is process-local.

For multiple backend instances, it will need to be replaced or extended with a distributed mechanism such as Redis.

This is included in the next backend phase.

---

# ⚡ Concurrency & Atomicity

Wallet operations are protected against concurrent modification.

The wallet uses optimistic locking through:

```java
@Version
```

Wallet mutations are also executed inside transactional service methods.

The intended consistency model is:

```text
Validate
   ↓
Lock / Version Check
   ↓
Update Wallet
   ↓
Create Ledger
   ↓
Commit Transaction
```

This prevents inconsistent wallet states during concurrent operations.

---

# 🧮 Wallet Consistency Principle

The system follows the fundamental wallet accounting rule:

```text
Current Balance
=
Initial Balance
+ Total Credits
- Total Debits
± Corrections
```

The transaction ledger provides the historical record required to verify wallet movements.

---

# 📋 Audit Logging

Important operations generate audit records.

Examples include:

```text
WALLET_CREDIT
WALLET_DEBIT
WITHDRAWAL_CREATED
WITHDRAWAL_PROCESSING
WITHDRAWAL_APPROVED
WITHDRAWAL_REJECTED
WITHDRAWAL_CANCELLED
```

Audit information can be used to investigate:

* Wallet changes
* Withdrawal actions
* Administrative actions
* Security-sensitive operations
* Financial discrepancies

---

# 🗄️ Database Architecture

The backend uses:

* MySQL
* Spring Data JPA
* Hibernate
* Flyway migrations

### Main database entities

```text
User
 │
 └── Wallet
       │
       └── WalletTransaction

PayoutMethod
 │
 └── PayoutOption

User
 │
 └── Withdrawal
       │
       └── WalletTransaction

User
 │
 └── AuditLog

User
 │
 └── WithdrawalIdempotency
```

---

# 🛠️ Database Migrations

Flyway migrations currently cover:

```text
V1  → Users
V2  → Wallets
V3  → Wallet Transactions
V4  → Payout Configuration
V5  → Withdrawals
V6  → Withdrawal Idempotency
V7  → Withdrawal Audit
V8  → Audit Log
```

Hibernate schema auto-generation is disabled in favor of controlled database migrations.

---

# 📌 Database Indexing

Indexes have been added around frequently accessed fields such as:

* User ID
* Wallet ID
* Transaction ID
* Reference ID
* Created timestamp
* Transaction status
* Withdrawal ID
* Withdrawal status
* Payout option
* Audit actor
* Audit target
* Idempotency key

This provides a foundation for scaling transaction and withdrawal queries.

---

# 🧪 Testing

The backend contains automated tests covering wallet, withdrawal, security, payout, concurrency, and rate limiting behavior.

Testing areas include:

### Wallet

* Wallet creation
* Wallet retrieval
* Wallet credit
* Wallet debit
* Insufficient balance
* Transaction history
* Wallet summary
* Wallet isolation
* JWT isolation
* Wallet consistency
* Concurrent wallet operations
* Atomicity

### Withdrawal

* Withdrawal creation
* Withdrawal validation
* Withdrawal eligibility
* Insufficient balance
* Withdrawal lifecycle
* Withdrawal cancellation
* Withdrawal rejection
* Withdrawal reversal
* Concurrent withdrawals
* Ownership protection

### Payout

* Payout configuration
* Payout option validation
* Payout detail validation

### Security

* JWT authentication
* Authorization
* User isolation
* Protected endpoints
* Rate limiting

### Idempotency

* Duplicate withdrawal request
* Same-key same-request behavior
* Same-key different-request conflict

---

# 📊 Current Test Evidence

The repository contains generated test execution reports showing:

```text
Tests:     100
Failures:  0
Errors:    0
Skipped:   0
```

These reports represent the test execution artifacts currently included in the project.

---

# 📚 API Documentation

The project includes API documentation and OpenAPI/Swagger configuration.

Swagger/OpenAPI is available during local development through the configured Springdoc integration.

The repository also contains:

```text
API_DOCUMENTATION.md
```

The documentation will be synchronized and expanded further during the final backend completion phase.

---

# 🧱 Project Structure

```text
src/
└── main/
    └── java/
        └── com/
            └── veloop/
                └── rewards/
                    ├── audit/
                    ├── auth/
                    ├── common/
                    ├── config/
                    ├── idempotency/
                    ├── payout/
                    ├── security/
                    ├── user/
                    ├── wallet/
                    └── withdrawal/
```

The project follows a domain-oriented Spring Boot architecture.

---

# 💻 Technology Stack

## Backend

* Java 21
* Spring Boot 3.5.x
* Spring Security
* Spring Data JPA
* Hibernate
* Jakarta Validation
* JWT
* BCrypt
* Flyway
* MySQL
* Maven
* Springdoc OpenAPI
* Spring Boot Actuator

## Testing

* JUnit
* Spring Boot Test
* MockMvc
* Integration Testing

---

# 🔧 Local Development

## Prerequisites

Install:

* Java 21+
* Maven
* MySQL
* Git

---

## Clone Repository

```bash
git clone <repository-url>
cd veloop-rewards
```

---

## Configure Database

Create a local MySQL database.

Example:

```sql
CREATE DATABASE veloop_rewards;
```

Configure the database connection using environment-specific application configuration.

---

## Configure Environment Variables

The final project should use environment variables for sensitive configuration.

Example:

```env
DB_URL=jdbc:mysql://localhost:3306/veloop_rewards
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=your_secure_secret
```

> Production secrets must never be committed to Git.

---

## Run Application

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

---

# 🔐 Security Configuration

The production configuration should provide:

* Strong database credentials
* Strong JWT secret
* Environment-based secrets
* Restricted database access
* HTTPS
* Production logging configuration
* Secure CORS configuration
* Distributed rate limiting where required

---

# 🧭 Next Development Phase

## `BACKEND-PHASE-02`

### Production Hardening & 100% Requirement Completion

The next phase is focused specifically on completing the remaining backend requirements before any frontend development begins.

The objective is:

> **Move the backend from a strong functional implementation to a complete requirement-aligned and production-oriented backend foundation.**

---

## Phase 02.1 — Configuration & Environment Hardening

### Tasks

* [ ] Create `.env.example`
* [ ] Remove development credentials from committed configuration
* [ ] Remove fallback JWT secrets
* [ ] Move secrets to environment variables
* [ ] Separate development and production configuration
* [ ] Review CORS configuration
* [ ] Review production logging
* [ ] Review actuator exposure
* [ ] Review sensitive error messages

### Checkpoint

```text
CONFIGURATION-HARDENED
```

---

# Phase 02.2 — Complete Payout Configuration

### Tasks

* [ ] Define complete payout option structure
* [ ] Add required options for Amazon Gift Cards
* [ ] Add required options for Google Play Gift Cards
* [ ] Define PayPal configuration behavior
* [ ] Add method-specific validation
* [ ] Add payout eligibility rules
* [ ] Add active/inactive configuration management
* [ ] Ensure all payout configuration is backend controlled

### Checkpoint

```text
PAYOUT-CONFIGURATION-COMPLETE
```

---

# Phase 02.3 — Withdrawal & Wallet Accounting Completion

### Tasks

* [ ] Verify every withdrawal path creates the correct ledger entry
* [ ] Verify rejection reversal
* [ ] Verify cancellation reversal
* [ ] Verify correction transactions
* [ ] Review `withdrawnVes` accounting
* [ ] Add accounting consistency checks
* [ ] Verify wallet balance against transaction ledger
* [ ] Add additional reconciliation tests

### Checkpoint

```text
WALLET-ACCOUNTING-COMPLETE
```

---

# Phase 02.4 — Distributed Rate Limiting

The current implementation uses in-memory fixed-window rate limiting.

This is sufficient for a single application instance but does not provide shared limits across multiple instances.

### Tasks

* [ ] Introduce Redis-based rate limiting
* [ ] Preserve endpoint-specific limits
* [ ] Preserve authenticated-user limits
* [ ] Preserve IP-based limits where required
* [ ] Handle Redis failures safely
* [ ] Add distributed rate-limit tests

### Target Architecture

```text
Client
   ↓
Load Balancer
   ↓
┌──────────────┬──────────────┐
│ Backend #1   │ Backend #2   │
└──────┬───────┴───────┬──────┘
       │               │
       └───────┬───────┘
               ↓
             Redis
```

### Checkpoint

```text
DISTRIBUTED-RATE-LIMITING-COMPLETE
```

---

# Phase 02.5 — Fraud & Abuse Protection

The original requirements include security and fraud-related protection.

### Planned capabilities

* [ ] Withdrawal frequency checks
* [ ] Suspicious withdrawal detection
* [ ] Repeated failed request detection
* [ ] Abnormal wallet activity detection
* [ ] Account-level withdrawal restrictions
* [ ] Suspicious payout detail detection
* [ ] Administrative review flags
* [ ] Audit trail for fraud-related decisions

### Checkpoint

```text
FRAUD-PROTECTION-COMPLETE
```

---

# Phase 02.6 — Financial Reconciliation

A production wallet should not rely only on transaction creation.

The system should also be able to verify that wallet balances agree with the ledger.

### Tasks

* [ ] Build wallet reconciliation service
* [ ] Calculate expected balance from ledger
* [ ] Compare expected vs actual balance
* [ ] Detect mismatches
* [ ] Record reconciliation failures
* [ ] Add administrative reconciliation endpoint/report
* [ ] Add automated reconciliation tests

### Core Formula

```text
Expected Balance
=
Opening Balance
+ Credits
- Debits
± Corrections
```

### Checkpoint

```text
FINANCIAL-RECONCILIATION-COMPLETE
```

---

# Phase 02.7 — Payout Processing Architecture

Currently the backend manages the withdrawal lifecycle.

For a larger production system, payout processing should be decoupled from the request API.

### Planned architecture

```text
Withdrawal API
      ↓
Withdrawal Service
      ↓
Database Transaction
      ↓
PENDING
      ↓
Message Queue
      ↓
Payout Worker
      ↓
External Payout Provider
      ↓
Processing Result
      ↓
APPROVED / REJECTED
```

### Tasks

* [ ] Introduce queue-based processing
* [ ] Create payout job
* [ ] Implement retry strategy
* [ ] Implement dead-letter handling
* [ ] Implement provider failure handling
* [ ] Make payout processing idempotent
* [ ] Add payout provider abstraction

### Checkpoint

```text
PAYOUT-PROCESSING-COMPLETE
```

---

# Phase 02.8 — Monitoring & Observability

### Tasks

* [ ] Improve Spring Boot Actuator configuration
* [ ] Add health checks
* [ ] Add application metrics
* [ ] Add wallet operation metrics
* [ ] Add withdrawal metrics
* [ ] Add rate-limit metrics
* [ ] Add failed transaction metrics
* [ ] Add structured logging
* [ ] Add correlation/request IDs
* [ ] Add operational dashboards where required

### Important metrics

```text
wallet.credit.success
wallet.debit.success
wallet.debit.failure
withdrawal.created
withdrawal.approved
withdrawal.rejected
withdrawal.cancelled
withdrawal.failure
rate_limit.blocked
ledger.reconciliation.failure
```

### Checkpoint

```text
OBSERVABILITY-COMPLETE
```

---

# Phase 02.9 — API & Documentation Completion

### Tasks

* [ ] Synchronize `API_DOCUMENTATION.md` with current source
* [ ] Document every endpoint
* [ ] Document authentication requirements
* [ ] Document request bodies
* [ ] Document response structures
* [ ] Document error responses
* [ ] Document pagination
* [ ] Document idempotency
* [ ] Document rate limiting
* [ ] Document withdrawal lifecycle
* [ ] Document payout configuration
* [ ] Document admin APIs

### Checkpoint

```text
API-DOCUMENTATION-COMPLETE
```

---

# Phase 02.10 — Database & Architecture Documentation

### Tasks

* [ ] Create database schema documentation
* [ ] Document entity relationships
* [ ] Document indexes
* [ ] Document wallet accounting
* [ ] Document withdrawal lifecycle
* [ ] Document concurrency strategy
* [ ] Document idempotency strategy
* [ ] Document audit strategy
* [ ] Document security architecture

### Checkpoint

```text
ARCHITECTURE-DOCUMENTATION-COMPLETE
```

---

# Phase 02.11 — Final Security & Requirement Audit

Perform a requirement-by-requirement audit against the original VELoop specification.

### Verify

* [ ] Backend is source of truth
* [ ] All currencies are backend controlled
* [ ] Ledger exists for all wallet changes
* [ ] Balance validation is server-side
* [ ] Withdrawal amount cannot be manipulated
* [ ] Payout options are backend controlled
* [ ] User isolation works
* [ ] JWT authentication works
* [ ] Authorization works
* [ ] Rate limiting works
* [ ] Idempotency works
* [ ] Duplicate withdrawal protection works
* [ ] Concurrent withdrawal protection works
* [ ] Audit records exist
* [ ] Reversal logic is correct
* [ ] Financial reconciliation works
* [ ] Error handling is safe
* [ ] Configuration is secure
* [ ] Database indexes are appropriate
* [ ] Tests cover critical financial flows
* [ ] API documentation matches implementation

### Final Checkpoint

```text
BACKEND-REQUIREMENTS-AUDIT-COMPLETE
```

---

# 🏁 Final Backend Goal

The backend development will be considered complete only after reaching:

```text
BACKEND-COMPLETE-100%
```

This checkpoint means:

```text
Requirements
     ↓
Implementation
     ↓
Security
     ↓
Validation
     ↓
Concurrency
     ↓
Idempotency
     ↓
Accounting
     ↓
Fraud Protection
     ↓
Reconciliation
     ↓
Rate Limiting
     ↓
Monitoring
     ↓
Documentation
     ↓
Testing
     ↓
Final Requirement Audit
     ↓
BACKEND-COMPLETE-100%
```

Only after this checkpoint will frontend development begin.

---

# 🌐 Frontend Status

## Not Started

Frontend development is intentionally postponed until the backend reaches the final requirement-completion checkpoint.

Planned frontend capabilities will consume the backend APIs for:

* Authentication
* Wallet dashboard
* Wallet balances
* Transaction history
* Withdrawal
* Payout selection
* Payout details
* Withdrawal status
* User account information
* Future reward features

The frontend will **not** become the source of truth for financial information.

---

# 📈 Future Scalability

The system is being designed with future growth in mind.

Target scenario:

```text
1,000 Users
      ↓
10,000 Users
      ↓
100,000 Users
      ↓
1,000,000+ Users
```

Future scalability considerations include:

* Database indexing
* Connection pooling
* Optimistic locking
* Atomic transactions
* Redis caching
* Distributed rate limiting
* Message queues
* Asynchronous payout processing
* Horizontal scaling
* Database optimization
* Read/write separation where required
* Monitoring
* Reconciliation
* Fraud detection
* Auditability

---

# 🧠 Engineering Principles

This project follows several important backend engineering principles.

### 1. Backend as Source of Truth

The client cannot determine financial values.

### 2. Atomic Financial Operations

Wallet changes and ledger records must remain consistent.

### 3. Idempotency

Retrying a sensitive operation should not create unintended duplicate financial transactions.

### 4. User Isolation

A user can only access their own wallet and withdrawals unless explicitly authorized.

### 5. Auditability

Important financial operations must be traceable.

### 6. Defensive Validation

Client input is treated as untrusted.

### 7. Concurrency Safety

Concurrent wallet operations must not produce invalid balances.

### 8. Controlled Configuration

Payout values and redemption rules belong to the backend.

### 9. Test Critical Financial Paths

Wallet and withdrawal operations require stronger testing than ordinary CRUD functionality.

### 10. Design for Growth

The architecture should be capable of evolving from a single backend instance toward a distributed system.

---

# 📊 Development Roadmap

```text
PHASE 01
Core Wallet & Withdrawal
        │
        ▼
BACKEND-CHECKPOINT-01
        │
        │  ✅ CURRENT
        ▼
PHASE 02
Production Hardening
        │
        ├── Configuration Hardening
        ├── Payout Completion
        ├── Accounting Completion
        ├── Distributed Rate Limiting
        ├── Fraud Protection
        ├── Financial Reconciliation
        ├── Queue-Based Payout
        ├── Monitoring
        ├── API Documentation
        ├── Architecture Documentation
        └── Final Security Audit
        │
        ▼
BACKEND-COMPLETE-100%
        │
        ▼
PHASE 03
Frontend Development
        │
        ▼
PHASE 04
Full Platform Integration
```

---

# 📌 Project Resume / Portfolio Description

### VELoop Rewards — Wallet & Withdrawal Backend

Developing a secure, scalable wallet and withdrawal backend using **Java, Spring Boot, Spring Security, JWT, JPA/Hibernate, MySQL and Flyway**. Implemented backend-authoritative multi-currency wallet management, transaction ledger, atomic wallet operations, optimistic locking, payout configuration, withdrawal lifecycle management, idempotency, rate limiting, user isolation, audit logging, validation, database migrations and automated integration testing. The backend is currently undergoing production hardening, financial reconciliation, fraud protection, distributed rate limiting and scalability improvements before frontend development.

---

# 💼 Resume Highlights

* Developed a **backend-authoritative multi-currency wallet system** using Java and Spring Boot.
* Implemented a **transaction ledger** with balance-before and balance-after tracking for financial traceability.
* Implemented **atomic wallet credit/debit operations** with transactional consistency and optimistic locking.
* Built a complete **withdrawal lifecycle** with payout configuration, eligibility validation, cancellation, approval, rejection and balance reversal.
* Implemented **idempotency protection** to prevent duplicate withdrawal requests.
* Implemented **JWT authentication, role-based authorization and user-level data isolation**.
* Implemented **rate limiting for sensitive authentication, wallet and withdrawal APIs**.
* Added **audit logging and database indexing** for financial and operational traceability.
* Built automated tests covering wallet consistency, concurrency, withdrawal behavior, payout validation, isolation and rate limiting.
* Designed the architecture for future **Redis, queue-based payout processing, fraud detection, reconciliation and horizontal scalability**.

---

# 📍 Current Project Position

```text
┌─────────────────────────────────────────────┐
│           VELOOP REWARDS BACKEND            │
├─────────────────────────────────────────────┤
│ Core Wallet System             ✅            │
│ Multi-Currency Wallet          ✅            │
│ Transaction Ledger             ✅            │
│ Authentication                 ✅            │
│ Authorization                  ✅            │
│ Payout Configuration           ✅            │
│ Withdrawal System              ✅            │
│ Idempotency                    ✅            │
│ Concurrency Protection         ✅            │
│ Rate Limiting                  ✅            │
│ Audit Logging                  ✅            │
│ Automated Testing              ✅            │
├─────────────────────────────────────────────┤
│ Production Hardening            🚧           │
│ Fraud Protection                🚧           │
│ Reconciliation                  🚧           │
│ Distributed Rate Limiting       🚧           │
│ Queue-Based Payout              🚧           │
│ Monitoring                      🚧           │
├─────────────────────────────────────────────┤
│ Backend 100% Completion         🎯           │
│ Frontend Development            ⏳           │
└─────────────────────────────────────────────┘
```

---

# ⚠️ Development Status

This repository represents an **actively developed backend project**.

The current implementation already contains the core wallet and withdrawal functionality, but the backend is **not being presented as fully production-complete yet**.

The remaining development work is intentionally tracked through the next backend phase.

The final milestone is:

> **`BACKEND-COMPLETE-100%`**

After this milestone is verified against the complete requirements, frontend development will begin.

---

# 👨‍💻 Project Focus

This project demonstrates practical backend engineering concepts including:

```text
Java
Spring Boot
REST APIs
Spring Security
JWT
JPA / Hibernate
MySQL
Flyway
Transactions
Financial Ledger
Concurrency
Optimistic Locking
Idempotency
Rate Limiting
Authorization
Audit Logging
Validation
Integration Testing
Database Design
Scalability
Distributed Systems
Production Hardening
```

---

# 📜 License

This project is currently under active development.

License and distribution terms will be finalized before public production release.
