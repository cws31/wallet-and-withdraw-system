# VELoop Rewards — Wallet & Withdrawal Backend

> **Status:** IN PROGRESS
> **Current Checkpoint:** `V3-WITHDRAWAL-SYSTEM-COMPLETE`
> **Current Phase:** Phase 6 — Withdrawal System **COMPLETED AND VERIFIED**
> **Next Phase:** Phase 7 — Idempotency + Withdrawal Concurrency
> **Verified Baseline:** 52/52 tests passing · `BUILD SUCCESS`

A secure, backend-driven Wallet & Withdrawal system for VELoop Rewards, built incrementally with phase-based checkpoints.

---

## ⚠️ Resume Instructions (Read First)

This README is the **resume point** of the project. To continue development:

1. **Do NOT start from project setup.** Do NOT rebuild: Authentication, JWT, Wallet, Wallet Ledger, Wallet Concurrency, Payout Configuration, Basic Withdrawal Flow, Withdrawal Deduction, Reversal, or Cancellation. These are **frozen, verified baselines**.
2. Run the baseline test suite:
   ```bash
   mvnw clean test
   ```
   Expected: `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0` → `BUILD SUCCESS`
3. Continue directly with **Phase 7 — Idempotency + Withdrawal Concurrency** (see [Section 14](#14-phase-7--idempotency--withdrawal-concurrency-next)).

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Core Principle](#2-core-principle--backend-is-the-source-of-truth)
3. [Development Status](#3-development-status)
4. [Checkpoint & Verification](#4-checkpoint--verification)
5. [Technology Stack](#5-technology-stack)
6. [Architecture & Package Structure](#6-architecture--package-structure)
7. [Database Migrations](#7-database-migrations)
8. [Wallet Core](#8-wallet-core)
9. [Payout Configuration](#9-payout-configuration)
10. [Withdrawal System (Phase 6)](#10-withdrawal-system-phase-6)
11. [Withdrawal Ledger & Reversals](#11-withdrawal-ledger--reversals)
12. [Validation, Ownership & Exceptions](#12-validation-ownership--exceptions)
13. [Test Status](#13-test-status)
14. [Phase 7 — Idempotency + Withdrawal Concurrency (Next)](#14-phase-7--idempotency--withdrawal-concurrency-next)
15. [Planned: Audit, Rate Limiting, Fraud Checks](#15-planned-audit-rate-limiting-fraud-checks)
16. [Planned Frontend](#16-planned-frontend)
17. [API Documentation](#17-api-documentation)
18. [Environment, Security & Production Restriction](#18-environment-security--production-restriction)
19. [Scalability Discussion](#19-scalability-discussion)
20. [Final Testing Requirements](#20-final-testing-requirements)
21. [Roadmap](#21-roadmap)
22. [Continuation Checkpoint](#22-continuation-checkpoint)
23. [Repository Structure](#23-expected-final-repository-structure)
24. [Final Submission Checklist](#24-final-submission-checklist)

---

## 1. Project Overview

VELoop Rewards uses multiple internal reward currencies and redemption mechanisms. This project independently builds a secure, scalable, backend-driven Wallet & Withdrawal system that manages:

- **Balances:** VES, SVES, Gems, Tokens, Spins
- **Wallet:** transactions, reward earnings, deductions, exchange-related balance changes, transaction history, balance validation
- **Withdrawals:** requests, status, eligibility, payout methods, payout options/vouchers, user payout details
- **Controls:** security and fraud-related checks, audit records

This is an **independent development/demo environment**. It does not connect to VELoop production systems or databases.

**High-level flow:**

```
User → Authentication → Wallet
                          ├── Current balances
                          ├── Transaction history
                          └── Withdrawal / Redeem
                                   ↓
                        Payout Configuration
                          ├── Payout methods
                          ├── Payout options / vouchers
                          ├── Required currency
                          └── Payout details
                                   ↓
                         Withdrawal Request
                          ├── Server-side validation
                          ├── Balance validation
                          ├── Wallet deduction
                          └── Ledger transaction
                                   ↓
                               PENDING
                          ├── PROCESSING ── APPROVED
                          │             └── REJECTED
                          └── CANCELLED
```

Idempotency and advanced withdrawal concurrency protections are handled in **Phase 7**.

---

## 2. Core Principle — Backend Is the Source of Truth

The frontend is only an interface and **must never be trusted for financial values**. Changing e.g. `VES: 1000 → 100000` in browser storage or JavaScript must never change the real balance.

**The backend owns:** wallet balances, reward amounts, required VES, payout values and availability, withdrawal eligibility and status, transaction creation, balance validation, authorization, idempotency, security checks, audit records.

**Allowed frontend request:**
```json
{ "payoutMethodId": 1, "payoutOptionId": 1, "payoutDetails": "user@upi" }
```

**Never trusted:**
```json
{ "amount": 19500 }
```
The backend resolves the real payout configuration from the database.

---

## 3. Development Status

### Completed

| Area | Delivered |
|------|-----------|
| **Foundation** | Java 21, Spring Boot 3.5.16, Maven, MySQL 8.x, Spring Data JPA, Hibernate, Flyway, Spring Security, JWT, Jakarta Bean Validation, Springdoc OpenAPI, Swagger UI |
| **Authentication** | Registration, request validation, BCrypt hashing, login, JWT generation, JWT filter, SecurityContext integration, protected APIs, authentication/authorization error handling, global exception handling, standard API response format, OpenAPI docs |
| **Wallet Core** | Wallet DB, one-wallet-per-user, VES/SVES/Gems/Tokens/Spins, retrieval, auto-creation after registration, credit, debit, server-side balance validation, insufficient/negative balance protection, transaction ledger, transaction types/status/history/pagination, wallet summary, JWT user isolation, admin-only mutations, optimistic locking, atomic wallet + ledger operations, concurrent balance protection, DB indexing, integration tests |
| **Payout Configuration** | Method & option DB, entities, repositories, response DTOs, configuration service, active method/option filtering, REST API, Swagger docs, integration tests |
| **Withdrawal (Phase 6)** | Migration, entity, status enum, repository, request/response DTOs, service, REST API, backend payout validation, method/option relationship validation, immediate VES deduction, withdrawal ledger transaction, insufficient balance protection, processing/approval/rejection workflows, rejection reversal, user cancellation + reversal, ownership protection, state validation, not-found handling, integration tests |

---

## 4. Checkpoint & Verification

**Phase history:**

```
Phase 1 — Project Setup
   ↓
Phase 2 — Database + Flyway
   ↓
Phase 3 — Authentication + JWT
   ↓
Phase 3.5 — OpenAPI / Swagger
   ↓
Phase 4 — Wallet Core  →  V1-WALLET-CORE-COMPLETE
   ↓
Phase 5 — Payout Configuration  →  V2-PAYOUT-CONFIGURATION-COMPLETE
   ↓
Phase 6 — Withdrawal System  →  V3-WITHDRAWAL-SYSTEM-COMPLETE
   ↓
52/52 TESTS PASSING
   ↓
NEXT: PHASE 7
```

**Verified ✅:** project setup, Java 21, Spring Boot 3.5.16, Maven build, MySQL, Flyway, users migration/entity, registration, login, JWT + filter, SecurityContext, authentication, authorization, global exception handling, OpenAPI/Swagger, wallet migration/entity/repository, auto wallet creation, retrieval, credit, debit, balance validation, insufficient balance protection, multi-currency, ledger, history, pagination, summary, user isolation (JWT wallet isolation), admin wallet authorization, atomicity, optimistic locking, concurrent debit protection, ledger consistency, payout methods/options/service/API/authentication/tests, withdrawal migration/entity/repository/service/API, wallet deduction integration, withdrawal ledger integration, approval, rejection, rejection reversal, cancellation, cancellation reversal, ownership protection, state validation, not-found handling.

**Test results:** Withdrawal integration tests **14/14** · Full regression suite **52/52**.

---

## 5. Technology Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3.5.16, Maven, Spring Web, Spring Data JPA, Hibernate, Spring Security, JJWT 0.12.6, Jakarta Bean Validation, Flyway, Springdoc OpenAPI, Swagger UI |
| Database | MySQL 8.x |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, integration testing, Swagger UI · *Planned:* Testcontainers (infrastructure hardening), Postman |
| Frontend (planned) | React, Vite, Bootstrap / CSS |

---

## 6. Architecture & Package Structure

```
React + Vite ──REST + JWT──► Spring Boot API
                                   │
                  ┌────────────────┼────────────────┐
                  ▼                ▼                ▼
                Auth            Wallet           Payout
                  │                │                │
                  ▼                ▼                ▼
                User            Ledger        Configuration
                                   └───────┬────────┘
                                           ▼
                                      Withdrawal
                                           ▼
                                   Spring Data JPA
                                           ▼
                                  MySQL  ◄── Flyway
```

```
src/main/java/com/veloop/rewards/
├── config/          SecurityConfig, OpenApiConfig, RateLimitConfig
├── security/        JwtAuthenticationFilter, JwtService, CustomUserDetailsService, SecurityExceptionHandler
├── auth/            controller, service, dto, mapper
├── user/            controller, service, repository, entity, dto
├── wallet/          controller, service, repository, entity, dto, enums
├── payout/
│   ├── controller/  PayoutConfigurationController
│   ├── service/     PayoutConfigurationService
│   ├── repository/  PayoutMethodRepository, PayoutOptionRepository
│   ├── entity/      PayoutMethod, PayoutOption
│   └── dto/         PayoutMethodResponse, PayoutOptionResponse
├── withdrawal/
│   ├── controller/  WithdrawalController
│   ├── service/     WithdrawalService
│   ├── repository/  WithdrawalRepository
│   ├── entity/      Withdrawal
│   ├── dto/         WithdrawalCreateRequest, WithdrawalResponse
│   └── enums/       WithdrawalStatus
├── audit/           service, repository, entity          (planned)
├── idempotency/     service, repository, entity          (planned)
└── common/          exception, response, util, enums
```

---

## 7. Database Migrations

```
src/main/resources/db/migration/
├── V1__create_users.sql                  → Users
├── V2__create_wallets.sql                → Wallets
├── V3__create_wallet_transactions.sql    → Wallet transactions / ledger
├── V4__create_payout_configuration.sql   → Payout methods + options
└── V5__create_withdrawals.sql            → Withdrawals
```

**Future:** `V6+` → Idempotency, Audit, additional security/infrastructure.

> **Do not modify already-applied migrations** unless there is a deliberate migration strategy. Schema changes should use a **new** Flyway migration.

---

## 8. Wallet Core

**Currencies:** `VES`, `SVES`, `GEMS`, `TOKENS`, `SPINS` — each has a separate balance. `BigDecimal` is used (no floating point).

**Wallet fields:** `id`, `userId`, `ves`, `sves`, `gems`, `tokens`, `spins`, `withdrawnVes`, `createdAt`, `updatedAt`, `version`

**Wallet table (`V2`):** `user_id UNIQUE`, `user_id → users.id` → one user, one wallet.
**Optimistic locking:** `@Version private Long version;` protects against concurrent modification.

### Ledger (`V3` — `wallet_transactions`)

Fields: `transactionId`, `userId`, `walletId`, `currency`, `transactionType`, `amount`, `balanceBefore`, `balanceAfter`, `source`, `referenceId`, `status`, `description`, `metadata`, `createdAt`.
Every successful wallet credit/debit creates a ledger record.

### Transaction Types

| Direction | Types |
|-----------|-------|
| **Credit** | `REWARD`, `BONUS`, `REFERRAL`, `DAILY_REWARD`, `AD_REWARD`, `GAME_REWARD`, `ADMIN_CREDIT`, `EXCHANGE_CREDIT` |
| **Debit** | `WITHDRAWAL`, `EXCHANGE_DEBIT`, `ADMIN_DEBIT`, `CORRECTION` |

These support future ads, referrals, daily rewards, games, spins, exchanges, withdrawals, and admin operations.

### Wallet Security

`JWT → Authenticated User ID → WalletService → User's Wallet`. The backend does not trust a client-provided `userId`; User A cannot access User B's wallet. `POST /api/wallet/credit` and `POST /api/wallet/debit` are **ADMIN only** (normal users get `403`).

---

## 9. Payout Configuration

Backend-controlled. Relationship: `payout_methods 1 : N payout_options`.

- **PayoutMethod:** `id`, `code`, `name`, `active`, `createdAt`, `updatedAt`
- **PayoutOption:** `id`, `method`, `payoutAmount`, `currency`, `currencyAmount`, `active`, `createdAt`, `updatedAt`

**Current development configuration:**

| Method | Status |
|--------|--------|
| `UPI` | ACTIVE |
| `AMAZON_GIFT_CARD` | ACTIVE |
| `GOOGLE_PLAY_GIFT_CARD` | ACTIVE |
| `PAYPAL` | INACTIVE |

**UPI options:**

| Payout Amount | Currency | Required VES |
|---------------|----------|--------------|
| ₹10 | INR | 2,400 |
| ₹25 | INR | 5,800 |
| ₹50 | INR | 10,000 |
| ₹100 | INR | 19,500 |
| ₹150 | INR | 28,500 |
| ₹300 | INR | 52,500 |
| ₹500 | INR | 80,500 |
| ₹1,000 | INR | 150,000 |

**Field mapping (important):**

| Field | Meaning |
|-------|---------|
| `PayoutOption.payoutAmount` | Actual cash / gift-card value |
| `PayoutOption.currency` | `INR` |
| `PayoutOption.currencyAmount` | **Required VES** |

Example: `payoutAmount = ₹10`, `currency = INR`, `currencyAmount = 2400 VES`.

**API:** `GET /api/payouts/configuration` (Bearer JWT, read-only) returns only **active** methods and options with payout amount, currency, and required VES.

---

## 10. Withdrawal System (Phase 6)

**Status:** COMPLETED AND VERIFIED · **Migration:** `V5__create_withdrawals.sql` · **Table:** `withdrawals`

**Entity fields:** `id`, `withdrawalId`, `userId`, `payoutMethodId`, `payoutOptionId`, `currency`, `currencyAmount`, `payoutAmount`, `payoutDetails`, `status`, `rejectionReason`, `reviewNote`, `transactionId`, `requestedAt`, `processedAt`, `createdAt`, `updatedAt`

### Statuses & Lifecycle

`PENDING`, `PROCESSING`, `APPROVED`, `REJECTED`, `CANCELLED`

```
PENDING ──► PROCESSING ──► APPROVED
   │             └───────► REJECTED
   ├────────────────────► APPROVED
   ├────────────────────► REJECTED
   └────────────────────► CANCELLED
```

User cancellation is allowed **only** while `PENDING`. Processing/approval/rejection are admin-protected.

### APIs

| Method | Endpoint | Access | Behavior |
|--------|----------|--------|----------|
| POST | `/api/withdrawals` | Authenticated user | Create withdrawal |
| GET | `/api/withdrawals?page=1&limit=20` | Authenticated user | Own withdrawals only |
| GET | `/api/withdrawals/{withdrawalId}` | Owner | Only if it belongs to the user |
| PATCH | `/api/withdrawals/{withdrawalId}/processing` | Admin | `PENDING → PROCESSING` |
| PATCH | `/api/withdrawals/{withdrawalId}/approve` | Admin | `PENDING/PROCESSING → APPROVED` (**no second deduction**) |
| PATCH | `/api/withdrawals/{withdrawalId}/reject` | Admin | `PENDING/PROCESSING → REJECTED`; **rejection reason required**; reverses deduction |
| PATCH | `/api/withdrawals/{withdrawalId}/cancel` | Owner | `PENDING → CANCELLED`; reverses deduction |

**Create request:**
```json
{ "payoutMethodId": 1, "payoutOptionId": 1, "payoutDetails": "test@upi" }
```
The backend resolves payout method, option, payout amount, required VES, currency, active status, and method/option relationship. The client never provides the amount to deduct.

### Deduction Strategy — Immediate VES Deduction

```
Validate payout configuration → Validate wallet balance → Debit VES
   → Create WITHDRAWAL ledger transaction → Create PENDING withdrawal
```
This occurs inside a single transactional service flow. Example: 10,000 VES − 2,400 VES = **7,600 VES**.

---

## 11. Withdrawal Ledger & Reversals

| Event | Type | Source | Reference | Currency / Amount |
|-------|------|--------|-----------|-------------------|
| Creation | `WITHDRAWAL` | `WITHDRAWAL` | Withdrawal ID | `VES` / required VES (description: "Wallet withdrawal") |
| Rejection reversal | `CORRECTION` | `WITHDRAWAL_REJECTED` | `<withdrawalId>-REVERSAL` | `VES` / +required VES |
| Cancellation reversal | `CORRECTION` | `WITHDRAWAL_CANCELLED` | `<withdrawalId>-CANCELLATION-REVERSAL` | `VES` / +required VES |

The withdrawal stores a reference to its wallet transaction (`transactionId`).

**Balance example:** 10,000 → withdrawal −2,400 → 7,600 → rejected/cancelled +2,400 → **10,000**. Both reversals are integration-tested.

---

## 12. Validation, Ownership & Exceptions

**Backend validates:** user existence, payout method existence + active status, payout option existence + active status, option belongs to selected method, wallet availability, required VES balance, withdrawal ownership, lifecycle state, required rejection reason. The frontend cannot override these.

**Ownership:** `GET /api/withdrawals/{id}` and `PATCH .../cancel` verify the authenticated user owns the withdrawal. Admin operations require `ROLE_ADMIN`.

**Business exceptions:** `InvalidWithdrawalRequestException`, `InvalidWithdrawalStateException`, `WithdrawalNotFoundException`, `WithdrawalOwnershipException`, `InsufficientBalanceException` (plus the global exception handler).

| Case | Result |
|------|--------|
| Invalid payout configuration | 400-level |
| Invalid withdrawal state | 400-level |
| Insufficient VES | 400-level |
| Withdrawal not found | 404 |
| Wrong user / ownership violation | 403 |

---

## 13. Test Status

**Full regression:** `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0` → `BUILD SUCCESS`

| Suite | Result |
|-------|--------|
| Wallet Core | PASS |
| Payout Configuration | PASS |
| Withdrawal System | 14/14 PASS |
| **Full suite** | **52/52 PASS** |

**Withdrawal test class:** `src/test/java/com/veloop/rewards/withdrawal/WithdrawalServiceIntegrationTest.java`

1. `shouldCreateWithdrawalAndDeductVes`
2. `shouldCreateWithdrawalLedgerTransaction`
3. `shouldRejectWithdrawalWhenBalanceIsInsufficient`
4. `shouldRejectInactivePayoutMethod`
5. `shouldRejectInactivePayoutOption`
6. `shouldRejectPayoutOptionFromDifferentMethod`
7. `shouldMoveWithdrawalToProcessing`
8. `shouldApproveWithdrawal`
9. `shouldRejectWithdrawalAndReverseVes`
10. `shouldCancelWithdrawalAndReverseVes`
11. `shouldRejectInvalidWithdrawalState`
12. `shouldRejectWithdrawalFromAnotherUser`
13. `shouldThrowNotFoundForInvalidWithdrawalId`
14. `shouldNotRejectApprovedWithdrawal`

---

## 14. Phase 7 — Idempotency + Withdrawal Concurrency (NEXT)

> The basic withdrawal flow is complete. **Do not rebuild Phase 6.** Phase 7 only hardens the existing withdrawal flow.

### 14.1 Idempotency

Prevent duplicate requests (double click, network retry). The system must not create **2 withdrawals** or **2 wallet deductions** for the same idempotent request.

```
Idempotency key → Existing request lookup → Return existing result
```

Expected components: `idempotency/{entity, repository, service}` plus a Flyway migration once the DB design is finalized (design against the existing withdrawal flow).

### 14.2 Withdrawal Concurrency

Example: balance = 10,000 VES; Request A → 8,000 and Request B → 8,000. The final state must **not** allow balance < 0, nor both deductions succeeding. Reuse and extend the existing wallet optimistic locking and transactional behavior rather than replacing it. Add dedicated withdrawal concurrency tests.

### 14.3 Required Tests (minimum)

1. Duplicate withdrawal request
2. Same idempotency key repeated
3. Different idempotency keys
4. Concurrent withdrawals against same wallet
5. Insufficient balance under concurrency
6. No duplicate wallet deduction
7. No duplicate withdrawal record
8. Ledger consistency
9. Safe retry behavior

**Rule:** Before Phase 7 → 52/52 PASS. After Phase 7 → all previous tests PASS **+** new Phase 7 tests PASS.

### 14.4 Implementation Rules

Do **not** start by rewriting `WalletService`, `PayoutConfigurationService`, `WithdrawalService`, or `WithdrawalController` unless a specific Phase 7 requirement demands it. Understand and extend first.

**Frozen baselines:** Wallet Core, Payout Configuration, Basic Withdrawal Creation, Withdrawal Deduction, Withdrawal Ledger, Approval, Rejection, Reversal, Cancellation.

### 14.5 First Task

Design the idempotency mechanism for `POST /api/withdrawals` **before writing code**, establishing:

```
Idempotency key → Database uniqueness → Existing request lookup
   → Safe retry behavior → Interaction with wallet transaction
   → Interaction with withdrawal transaction
```
Then implement and test incrementally.

---

## 15. Planned: Audit, Rate Limiting, Fraud Checks

**Audit logging (Phase 8):** events — Withdrawal Created / Approved / Rejected / Cancelled, Wallet Credit, Wallet Debit, Balance Correction, Payout Configuration Changed. Planned fields: `actorId`, `action`, `targetUserId`, `targetType`, `referenceId`, `metadata`, IP/session info where appropriate, `createdAt`. **Do not implement the full audit system in Phase 7** unless a Phase 7 dependency requires it.

**Rate limiting (security-hardening phase):** for authentication, wallet mutations, withdrawal, OTP/authentication endpoints. Status: *planned / incomplete*.

**Fraud & security checks (future):** account status, eligibility, duplicate request prevention, suspicious activity handling, server-side validation, payout/withdrawal validation, audit logging, reconciliation — introduced incrementally.

---

## 16. Planned Frontend

Small React/Vite demo. **Routes:** `/wallet`, `/payout` (optional `/login`). **The frontend must contain no financial business logic.**

- **Wallet:** fetch wallet, display balances and transactions, redeem action, navigate to payout, loading/error states, refresh after successful operations.
- **Payout:** fetch methods → select method → select option → enter details → confirmation → `POST` withdrawal.

---

## 17. API Documentation

A complete `API_DOCUMENTATION.md` documents endpoint, method, authentication, request, response, errors, and examples. Swagger/OpenAPI is already configured.

```
GET   /api/wallet
GET   /api/wallet/transactions
GET   /api/wallet/summary
POST  /api/wallet/credit
POST  /api/wallet/debit
GET   /api/payouts/configuration
POST  /api/withdrawals
GET   /api/withdrawals
GET   /api/withdrawals/{withdrawalId}
PATCH /api/withdrawals/{withdrawalId}/processing
PATCH /api/withdrawals/{withdrawalId}/approve
PATCH /api/withdrawals/{withdrawalId}/reject
PATCH /api/withdrawals/{withdrawalId}/cancel
```

---

## 18. Environment, Security & Production Restriction

**Never commit:** `.env`, real passwords, database credentials, JWT secrets, API keys, production credentials. Provide `.env.example` for configuration examples. Use a local/development database only.

**This project must not connect to VELoop production databases or services.**
```
Developer Frontend → Developer Backend → Developer MySQL
```
Production integration, if required, must be done later by authorized VELoop developers.

---

## 19. Scalability Discussion

Final architecture discussion must answer: *"If VELoop Rewards grows from 1,000 to 1,000,000 users, what changes are required?"* Cover: database transactions, atomic updates, ledger architecture, idempotency, indexing, queues, caching, rate limiting, fraud detection, audit logs, reconciliation, monitoring, scalability.

---

## 20. Final Testing Requirements

| # | Scenario | Status |
|---|----------|--------|
| 1 | Normal credit | ✅ |
| 2 | Normal withdrawal | ✅ |
| 3 | Insufficient balance | ✅ |
| 4 | Duplicate withdrawal / idempotency | ⏳ Phase 7 |
| 5 | Concurrent withdrawals | ⏳ Phase 7 |
| 6 | Invalid payout option | ✅ |
| 7 | Another user's wallet | ✅ |
| 8 | Rejected withdrawal reversal | ✅ |
| 9 | Frontend/API manipulation | ✅ |

Also verified: concurrent wallet updates ✅.

---

## 21. Roadmap

```
Phase 1  Project Setup
Phase 2  Database + Flyway
Phase 3  Authentication + JWT
Phase 3.5 OpenAPI / Swagger
Phase 4  Wallet Core                       → V1-WALLET-CORE-COMPLETE
Phase 5  Payout Configuration              → V2-PAYOUT-CONFIGURATION-COMPLETE
Phase 6  Withdrawal System                 → V3-WITHDRAWAL-SYSTEM-COMPLETE   ✅ (current)
Phase 7  Idempotency + Withdrawal Concurrency                                 ⏳ NEXT
Phase 8  Audit + Security Hardening
Phase 9  React Demonstration Frontend
Phase 10 API Documentation + Postman + Final Testing
Phase 11 Deployment / Demonstration
```

---

## 22. Continuation Checkpoint

**Checkpoint:** `V3-WITHDRAWAL-SYSTEM-COMPLETE` · **Baseline:** 52/52 PASS, BUILD SUCCESS

**Completed modules ✅:** Authentication, JWT, Authorization, OpenAPI/Swagger, Wallet (balances, ledger, transactions, pagination, summary, validation, atomicity, optimistic locking, concurrency, user isolation), Payout Configuration (methods, options, API, tests), Basic Withdrawal System (creation, deduction, ledger, processing, approval, rejection, reversal, cancellation, ownership, state validation), Withdrawal Integration Tests (14/14).

**Business rules already implemented:**

- **Payout mapping:** `payoutAmount` = actual INR payout value; `currencyAmount` = required VES (₹10 INR requires 2400 VES).
- **Deduction:** Creation → immediate VES deduction → `WITHDRAWAL` ledger → `PENDING` withdrawal.
- **Rejection:** `PENDING/PROCESSING → REJECTED` → VES reversal → `CORRECTION` ledger.
- **Cancellation:** `PENDING → CANCELLED` → VES reversal → `CORRECTION` ledger.
- **Approval:** `PENDING/PROCESSING → APPROVED`; **no second VES deduction.**
- **Ownership:** authenticated users can only access their own withdrawals.
- **Admin operations:** processing, approve, reject → `ADMIN` only.
- **User operation:** cancel → authenticated owner only, `PENDING` only.

**Exact next step:** run `mvnw clean test` (expect 52/52), then continue with **Phase 7 — Idempotency + Withdrawal Concurrency**, starting with the idempotency design for `POST /api/withdrawals` (see [14.5](#145-first-task)).

---

## 23. Expected Final Repository Structure

```
veloop-rewards-backend/
├── pom.xml
├── README.md
├── API_DOCUMENTATION.md
├── .env.example
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/com/veloop/rewards/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   └── test/java/com/veloop/rewards/
└── frontend/                    # React/Vite application
```

---

## 24. Final Submission Checklist

**Backend**
- [x] Wallet completely backend-driven
- [x] VES / SVES / Gems / Tokens / Spins server-authoritative
- [x] Wallet transactions stored; credits and debits create ledger records
- [x] Withdrawal requests stored
- [x] Payout options and values controlled by backend
- [x] Insufficient balance rejected
- [x] Concurrent wallet balance updates handled safely
- [x] Users cannot access another user's wallet
- [x] Authentication implemented; sensitive wallet APIs protected; wallet validation implemented
- [ ] Duplicate withdrawal prevented *(Phase 7)*
- [ ] Rate limiting fully implemented
- [ ] Audit logging implemented
- [ ] Withdrawal idempotency
- [ ] Withdrawal advanced concurrency tests
- [ ] Withdrawal fraud/security hardening

**Payout Configuration**
- [x] Method table, option table, entities, repositories, DTOs, service, API, active/inactive filtering, tests

**Withdrawal**
- [x] Table, entity, repository, DTOs, service, API
- [x] Backend payout validation, immediate VES deduction, withdrawal ledger transaction
- [x] Approval, rejection (+ reversal), cancellation (+ reversal) workflows
- [x] Ownership validation, state validation, integration tests
- [ ] Idempotency
- [ ] Advanced concurrency hardening

**Frontend**
- [ ] `/wallet`, `/payout`
- [ ] Backend-connected wallet, payout options, and withdrawal
- [ ] Loading states, error states, wallet refresh after withdrawal

**Documentation**
- [x] README development status
- [x] Architecture documentation
- [x] Current API documentation through OpenAPI/Swagger
- [x] Current test results documented
- [ ] `API_DOCUMENTATION.md` complete
- [ ] Database/model documentation
- [ ] Postman collection

**Security**
- [x] `.env` excluded, `.env.example` provided
- [x] No production credentials; no production database connection
- [ ] Rate limiting
- [ ] Audit logging
- [ ] Withdrawal idempotency
- [ ] Withdrawal fraud/security checks

**Delivery**
- [ ] GitHub repository
- [ ] Live frontend
- [ ] Live backend/API
- [ ] Demonstration/video
- [ ] Final test evidence

---

## Final Checkpoint

```
V3-WITHDRAWAL-SYSTEM-COMPLETE
        │
        ▼
52/52 TESTS PASSING
        │
        ▼
NEXT: PHASE 7 — IDEMPOTENCY + WITHDRAWAL CONCURRENCY
```

**The project must continue from Phase 7.** Previously completed Wallet Core, Payout Configuration, and basic Withdrawal System work are verified baselines and **must not be rebuilt**.