# VELoop Rewards — Wallet & Withdrawal Backend

> **Development Status:** IN PROGRESS
>
> A secure, backend-driven Wallet & Withdrawal system for VELoop Rewards, built incrementally with phase-based development checkpoints.
>
> **Current Checkpoint:** `BACKEND-CHECKPOINT-01 — CORE WALLET & WITHDRAWAL SYSTEM VERIFIED`
>
> **Current Verified Test Baseline:** `56/56 PASS`
>
> **Current Development Position:** Core Wallet + Payout + Withdrawal + Idempotency + Concurrency + Audit implementation completed and manually verified.
>
> **Next Development Area:** Backend Security Hardening / Remaining Backend Requirements
>
> **Important:** Do not rebuild previously completed Wallet, Payout, Withdrawal, Idempotency, Concurrency, or Audit functionality unless a new requirement specifically requires a change.

---

# 1. Project Overview

VELoop Rewards uses multiple internal reward currencies and redemption mechanisms.

The objective of this project is to independently build a secure, scalable and backend-driven Wallet & Withdrawal system that can manage:

* User wallet balances
* VEs
* SVEs
* Gems
* Tokens
* Spins
* Wallet transactions
* Reward earnings
* Wallet deductions
* Exchange-related balance changes
* Withdrawal requests
* Withdrawal status
* Payout methods
* Payout options / vouchers
* User payout details
* Transaction history
* Balance validation
* Withdrawal eligibility
* Security and fraud-related checks
* Audit records

The project is an **independent development/demo environment**.

It does **not** connect to VELoop production systems or production databases.

The backend is always the **single source of truth**.

The frontend must never be able to directly modify wallet balances.

For example, changing:

```text
VES: 1000 → 100000
```

in browser storage or frontend JavaScript must never change the actual wallet balance stored by the backend.

---

# 2. Important Development Rule

This project is being developed incrementally.

When continuing development:

1. Read this README.
2. Read the original project requirements.
3. Check the **Current Checkpoint** section.
4. Run the baseline test suite.
5. Continue from the stated **Next Development Area**.
6. Do not rebuild modules marked as completed and verified.
7. Only modify completed modules when a new requirement explicitly requires an extension or hardening.

The README is intended to function as the **project resume point**.

---

# 3. Project Objective

The final backend follows this high-level flow:

```text
User
 │
 ▼
Authentication
 │
 ▼
Wallet
 │
 ├── Current balances
 │
 ├── Transaction history
 │
 └── Withdrawal / Redeem
 │
 ▼
Payout Configuration
 │
 ├── Payout methods
 │
 ├── Payout options / vouchers
 │
 ├── Required currency
 │
 └── Payout details
 │
 ▼
Withdrawal Request
 │
 ├── Server-side validation
 ├── Balance validation
 ├── Idempotency validation
 ├── Concurrency protection
 ├── Wallet deduction
 ├── Ledger transaction
 └── Audit record
 │
 ▼
PENDING
 │
 ├── PROCESSING
 │      ├── APPROVED
 │      └── REJECTED
 │
 └── CANCELLED
```

The currently implemented withdrawal strategy is:

> **Immediate VES deduction when the withdrawal is created.**

If the withdrawal is rejected or cancelled, the deducted VES is reversed through a correction ledger transaction.

---

# 4. Core Design Principle — Backend Is the Source of Truth

The frontend is only an interface.

The backend owns:

* Current wallet balances
* Reward amounts
* Required VEs
* Payout values
* Payout availability
* Withdrawal eligibility
* Withdrawal status
* Transaction creation
* Balance validation
* Authorization
* Idempotency
* Security checks
* Audit records

The frontend must never be trusted for financial values.

For example, the frontend may send:

```json
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "user@upi"
}
```

The backend resolves the actual payout configuration from the database.

The frontend cannot send an arbitrary financial amount and force the backend to trust it.

---

# 5. Current Development Status

## Completed and Verified

### Project Foundation

* Java 21
* Spring Boot 3.5.16
* Maven
* MySQL 8.x
* Spring Data JPA
* Hibernate
* Flyway
* Spring Security
* JJWT 0.12.6
* Jakarta Bean Validation
* Springdoc OpenAPI
* Swagger UI

### Authentication Foundation

* User registration
* Request validation
* BCrypt password hashing
* Login
* JWT generation
* JWT authentication filter
* SecurityContext integration
* Protected APIs
* Authentication error handling
* Authorization error handling
* Global exception handling
* Standard API response format
* OpenAPI / Swagger documentation

### Wallet Core

* Wallet database
* One-wallet-per-user architecture
* VES
* SVES
* Gems
* Tokens
* Spins
* Wallet retrieval
* Automatic wallet creation
* Wallet credit
* Wallet debit
* Server-side balance validation
* Insufficient balance protection
* Negative balance protection
* Wallet transaction ledger
* Transaction types
* Transaction status
* Transaction history
* Transaction pagination
* Wallet summary
* JWT-based user isolation
* Admin-only wallet mutations
* Optimistic locking
* Atomic wallet operations
* Concurrent balance protection
* Database indexing
* Integration tests

### Payout Configuration

* Payout method database
* Payout option database
* Payout method entity
* Payout option entity
* Payout repositories
* Payout response DTOs
* Payout configuration service
* Active payout filtering
* Active payout option filtering
* Backend-controlled payout values
* Payout configuration REST API
* Swagger/OpenAPI documentation
* Payout configuration integration tests

### Withdrawal System

* Withdrawal database
* Withdrawal migration
* Withdrawal entity
* Withdrawal status enum
* Withdrawal repository
* Withdrawal request DTO
* Withdrawal response DTO
* Withdrawal service
* Withdrawal REST API
* Backend payout validation
* Backend payout option/method relationship validation
* Immediate VES deduction
* Withdrawal ledger transaction
* Insufficient balance protection
* Processing workflow
* Approval workflow
* Rejection workflow
* Rejection balance reversal
* User cancellation workflow
* Cancellation balance reversal
* Withdrawal ownership protection
* Withdrawal state validation
* Withdrawal not-found handling

### Phase 7 — Idempotency + Withdrawal Concurrency

**COMPLETED AND VERIFIED**

* Idempotency key support
* Idempotency database table
* Unique `(user_id, idempotency_key)` constraint
* Request fingerprinting
* Same-key/same-request safe retry
* Same-key/different-request conflict
* Missing idempotency key validation
* Idempotency exception handling
* Wallet optimistic locking
* Concurrent withdrawal protection
* Concurrent withdrawal integration test
* Duplicate withdrawal protection
* Safe retry behavior

### Phase 8 — Audit Logging

**COMPLETED AND VERIFIED**

* Generic `audit_log` table
* `AuditLog` entity
* `AuditLogRepository`
* `AuditLogService`
* Withdrawal-specific audit table
* Withdrawal audit service
* Wallet credit audit
* Wallet debit audit
* Withdrawal created audit
* Withdrawal processing audit
* Withdrawal approved audit
* Withdrawal rejected audit
* Admin actor tracking
* Target-user tracking
* Reference ID tracking
* Metadata tracking
* Audit timestamps

---

# 6. Current Checkpoint

## `BACKEND-CHECKPOINT-01`

### CORE WALLET & WITHDRAWAL SYSTEM VERIFIED

Current state:

```text
Phase 1
Project Setup
        ↓
Phase 2
Database + Flyway
        ↓
Phase 3
Authentication + JWT
        ↓
Phase 3.5
OpenAPI / Swagger
        ↓
Phase 4
Wallet Core
        ↓
V1-WALLET-CORE-COMPLETE
        ↓
Phase 5
Payout Configuration
        ↓
V2-PAYOUT-CONFIGURATION-COMPLETE
        ↓
Phase 6
Withdrawal System
        ↓
V3-WITHDRAWAL-SYSTEM-COMPLETE
        ↓
Phase 7
Idempotency + Withdrawal Concurrency
        ↓
V4-IDEMPOTENCY-CONCURRENCY-COMPLETE
        ↓
Phase 8
Audit Logging
        ↓
BACKEND-CHECKPOINT-01
        ↓
56/56 TESTS PASS
        ↓
MANUAL ADMIN API E2E VERIFIED
```

---

# 7. Current Verified Test Baseline

The current full regression suite:

```text
Tests run: 56
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Therefore:

```text
56/56 PASS
```

This is the current regression baseline.

Before future development:

```bash
mvn clean test
```

Expected baseline:

```text
56/56 PASS
BUILD SUCCESS
```

If the test count increases because new tests are added, the README must be updated to reflect the new baseline.

---

# 8. Manual API E2E Verification

In addition to automated tests, the complete wallet and withdrawal flows were manually verified through Swagger/API testing using an authenticated **ADMIN JWT**.

## Approved Withdrawal Flow

Verified:

```text
Wallet Credit
10,000 VES
        ↓
Payout Configuration
UPI / ₹10 / 2,400 VES
        ↓
Create Withdrawal
        ↓
2,400 VES deducted
        ↓
Wallet = 7,600 VES
        ↓
Ledger transaction created
        ↓
Withdrawal = PENDING
        ↓
PROCESSING
        ↓
APPROVED
```

Verified withdrawal:

```text
Withdrawal ID:
WD-5d8b0e22-8dbd-4425-a62a-4119fa2a3401

Payout:
₹10

Required VES:
2400

Transaction ID:
2985
```

---

# 9. Manual Rejection and Reversal Verification

A second fresh withdrawal was tested.

```text
Withdrawal ID:
WD-7302a686-1d86-4300-b551-fcf28b65050b
```

Flow:

```text
Wallet
7,600 VES
        ↓
Withdrawal
-2,400 VES
        ↓
5,200 VES
        ↓
REJECTED
        ↓
+2,400 VES reversal
        ↓
7,600 VES
```

The ledger contained:

```text
WITHDRAWAL
2400 VES
7600 → 5200
```

followed by:

```text
CORRECTION
2400 VES
5200 → 7600
```

The final wallet balance was verified as:

```text
7,600 VES
```

This proves the rejection reversal is not only returned by the API but actually reflected in the wallet and ledger.

---

# 10. Verified Audit Flow

Generic audit records were manually verified.

For the approved withdrawal:

```text
WALLET_DEBIT
WITHDRAWAL_CREATED
WITHDRAWAL_PROCESSING
WITHDRAWAL_APPROVED
```

For the rejected withdrawal:

```text
WALLET_DEBIT
WITHDRAWAL_CREATED
WITHDRAWAL_REJECTED
```

Withdrawal-specific audit was also verified.

Approved flow:

```text
CREATED
NULL → PENDING

PROCESSING
PENDING → PROCESSING

APPROVED
PROCESSING → APPROVED
```

Rejected flow:

```text
CREATED
NULL → PENDING

REJECTED
PENDING → REJECTED
```

Admin actor IDs were correctly recorded for administrative lifecycle actions.

---

# 11. Wallet Core

## Supported currencies

```text
VES
SVES
GEMS
TOKENS
SPINS
```

Wallet structure:

```text
Wallet
├── id
├── userId
├── ves
├── sves
├── gems
├── tokens
├── spins
├── withdrawnVes
├── createdAt
├── updatedAt
└── version
```

`BigDecimal` is used for wallet/reward values.

Optimistic locking:

```java
@Version
private Long version;
```

---

# 12. Wallet APIs

Implemented:

```http
GET /api/wallet
GET /api/wallet/transactions
GET /api/wallet/summary

POST /api/wallet/credit
POST /api/wallet/debit
```

Wallet mutations are protected and restricted to administrative/internal use.

Normal users cannot directly perform administrative wallet mutations.

---

# 13. Wallet Ledger

Table:

```text
wallet_transactions
```

Ledger stores:

```text
transactionId
userId
walletId
currency
transactionType
amount
balanceBefore
balanceAfter
source
referenceId
status
description
metadata
createdAt
```

Every successful wallet credit/debit creates a corresponding ledger record.

---

# 14. Transaction Types

## Credit

```text
REWARD
BONUS
REFERRAL
DAILY_REWARD
AD_REWARD
GAME_REWARD
ADMIN_CREDIT
EXCHANGE_CREDIT
```

## Debit / Adjustment

```text
WITHDRAWAL
EXCHANGE_DEBIT
ADMIN_DEBIT
CORRECTION
```

---

# 15. Wallet Security

Wallet APIs determine the user from the authenticated JWT.

The backend does not trust a client-provided `userId`.

```text
JWT
 ↓
Authenticated User ID
 ↓
WalletService
 ↓
User's Wallet
```

User A cannot access User B's wallet.

Wallet mutations are protected using admin authorization.

---

# 16. Payout Configuration

Database relationship:

```text
payout_methods
        │
        │ 1 : N
        ▼
payout_options
```

## PayoutMethod

```text
id
code
name
active
createdAt
updatedAt
```

## PayoutOption

```text
id
method
payoutAmount
currency
currencyAmount
active
createdAt
updatedAt
```

---

# 17. Current Payout Configuration

Current development configuration:

```text
UPI                    ACTIVE
AMAZON_GIFT_CARD       ACTIVE
GOOGLE_PLAY_GIFT_CARD  ACTIVE
PAYPAL                 INACTIVE
```

Current UPI options:

| Payout | Currency | Required VES |
| -----: | :------: | -----------: |
|    ₹10 |    INR   |        2,400 |
|    ₹25 |    INR   |        5,800 |
|    ₹50 |    INR   |       10,000 |
|   ₹100 |    INR   |       19,500 |
|   ₹150 |    INR   |       28,500 |
|   ₹300 |    INR   |       52,500 |
|   ₹500 |    INR   |       80,500 |
| ₹1,000 |    INR   |      150,000 |

Important mapping:

```text
PayoutOption.payoutAmount
        ↓
Actual payout value

PayoutOption.currency
        ↓
INR

PayoutOption.currencyAmount
        ↓
Required VES
```

Example:

```text
payoutAmount   = ₹10
currency       = INR
currencyAmount = 2400 VES
```

The backend resolves these values from the database.

---

# 18. Payout Configuration API

```http
GET /api/payouts/configuration
```

The API returns backend-controlled payout configuration.

It includes:

* Active payout methods
* Active payout options
* Payout amount
* Currency
* Required VES

Inactive methods/options are excluded.

---

# 19. Withdrawal System

## Withdrawal Model

The withdrawal entity stores:

```text
id
withdrawalId
userId
payoutMethodId
payoutOptionId
currency
currencyAmount
payoutAmount
payoutDetails
status
rejectionReason
reviewNote
transactionId
requestedAt
processedAt
createdAt
updatedAt
```

---

# 20. Withdrawal Statuses

```text
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
```

Implemented lifecycle:

```text
PENDING
   ├── PROCESSING
   │      ├── APPROVED
   │      └── REJECTED
   │
   ├── REJECTED
   │
   └── CANCELLED
```

Approval does not perform another wallet deduction.

---

# 21. Withdrawal APIs

Create:

```http
POST /api/withdrawals
```

History:

```http
GET /api/withdrawals?page=1&limit=20
```

Details:

```http
GET /api/withdrawals/{withdrawalId}
```

Admin processing:

```http
PATCH /api/withdrawals/{withdrawalId}/processing
```

Admin approval:

```http
PATCH /api/withdrawals/{withdrawalId}/approve
```

Admin rejection:

```http
PATCH /api/withdrawals/{withdrawalId}/reject
```

User cancellation:

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
```

---

# 22. Withdrawal Deduction Strategy

The selected architecture is:

> **Immediate VES deduction at withdrawal creation.**

Example:

```text
Initial:
10,000 VES

Withdrawal:
2,400 VES

Remaining:
7,600 VES
```

The creation flow is:

```text
Validate payout configuration
        ↓
Validate wallet balance
        ↓
Debit VES
        ↓
Create WITHDRAWAL ledger transaction
        ↓
Create PENDING withdrawal
        ↓
Create audit records
```

---

# 23. Withdrawal Rejection Reversal

When rejected:

```text
Withdrawal
    ↓
REJECTED
    ↓
Reverse original VES deduction
    ↓
Create CORRECTION ledger transaction
```

Reversal:

```text
Transaction Type:
CORRECTION

Source:
WITHDRAWAL_REJECTED

Reference:
<withdrawalId>-REVERSAL
```

Verified behavior:

```text
7600
 ↓
-2400
 ↓
5200
 ↓
REJECTED
 ↓
+2400
 ↓
7600
```

---

# 24. Withdrawal Cancellation Reversal

When a pending withdrawal is cancelled:

```text
PENDING
   ↓
CANCELLED
   ↓
Reverse VES deduction
   ↓
Create CORRECTION ledger transaction
```

Source:

```text
WITHDRAWAL_CANCELLED
```

Reference:

```text
<withdrawalId>-CANCELLATION-REVERSAL
```

---

# 25. Withdrawal Validation

Currently implemented validation includes:

* User existence
* Payout method existence
* Payout method active status
* Payout option existence
* Payout option active status
* Payout option belongs to selected method
* Wallet availability
* Required VES balance
* Withdrawal ownership
* Withdrawal lifecycle state
* Required rejection reason
* Request validation

The backend determines the actual required VES from the selected payout option.

---

# 26. Idempotency

## Status

```text
COMPLETED AND VERIFIED
```

Idempotency is implemented for:

```http
POST /api/withdrawals
```

The system uses an idempotency key.

Database uniqueness:

```text
(user_id, idempotency_key)
```

Request fingerprinting is also implemented.

Behavior:

### Same key + same request

```text
Return existing withdrawal
```

### Same key + different request

```text
409 Conflict
```

### Missing key

```text
400 Bad Request
```

This prevents duplicate wallet deductions and duplicate withdrawal creation.

---

# 27. Withdrawal Concurrency

## Status

```text
COMPLETED AND VERIFIED
```

Wallet optimistic locking is used:

```java
@Version
private Long version;
```

Concurrent withdrawal attempts are protected.

The concurrency integration test verifies that simultaneous withdrawals cannot both successfully deduct the same wallet balance.

The existing wallet transaction and withdrawal transaction behavior is preserved.

---

# 28. Audit Logging

## Status

```text
COMPLETED AND VERIFIED
```

Generic table:

```text
audit_log
```

Fields:

```text
id
actor_id
target_user_id
target_type
action
reference_id
metadata
created_at
```

Implemented audit events include:

```text
WALLET_CREDIT
WALLET_DEBIT

WITHDRAWAL_CREATED
WITHDRAWAL_PROCESSING
WITHDRAWAL_APPROVED
WITHDRAWAL_REJECTED
```

---

# 29. Withdrawal-Specific Audit

Existing withdrawal audit is preserved separately from the generic audit log.

Fields:

```text
id
withdrawal_id
user_id
action
old_status
new_status
performed_by
description
created_at
```

Verified lifecycle examples:

```text
CREATED
NULL → PENDING

PROCESSING
PENDING → PROCESSING

APPROVED
PROCESSING → APPROVED
```

and:

```text
CREATED
NULL → PENDING

REJECTED
PENDING → REJECTED
```

---

# 30. Database Migrations

Current migration sequence includes the core database plus later hardening migrations.

Conceptually:

```text
V1 → Users
V2 → Wallets
V3 → Wallet Transactions / Ledger
V4 → Payout Configuration
V5 → Withdrawals
V6 → Withdrawal Idempotency
V7 → Withdrawal Audit
V8 → Generic Audit Log
```

Do not modify already-applied Flyway migrations casually.

New schema changes should normally use a new migration.

---

# 31. Current Package Architecture

```text
src/main/java/com/veloop/rewards/

├── config/
│
├── security/
│
├── auth/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   └── mapper/
│
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── wallet/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── enums/
│
├── payout/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── withdrawal/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── enums/
│
├── audit/
│   ├── service/
│   ├── repository/
│   └── entity/
│
├── idempotency/
│   ├── service/
│   ├── repository/
│   └── entity/
│
└── common/
    ├── exception/
    ├── response/
    ├── util/
    └── enums/
```

---

# 32. Current Architecture

```text
                    React + Vite
                         │
                         │ REST + JWT
                         ▼
               ┌─────────────────────┐
               │   Spring Boot API   │
               └──────────┬──────────┘
                          │
             ┌────────────┼─────────────┐
             │            │             │
             ▼            ▼             ▼
           Auth         Wallet        Payout
             │            │             │
             ▼            ▼             ▼
           User         Ledger     Configuration
                          │             │
                          └──────┬──────┘
                                 │
                                 ▼
                           Withdrawal
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
                    ▼            ▼            ▼
              Idempotency     Audit      Security
                    │            │
                    └──────┬─────┘
                           │
                           ▼
                    Spring Data JPA
                           │
                           ▼
                         MySQL
                           ▲
                           │
                         Flyway
```

---

# 33. Test Coverage

The current automated regression suite is:

```text
56/56 PASS
```

Important verified areas include:

* Normal wallet credit
* Normal wallet debit
* Insufficient balance
* Wallet transaction creation
* Wallet history
* Wallet summary
* User isolation
* JWT isolation
* Admin wallet authorization
* Payout configuration
* Invalid payout method
* Invalid payout option
* Payout method/option mismatch
* Normal withdrawal
* Withdrawal ledger
* Processing
* Approval
* Rejection
* Rejection reversal
* Cancellation
* Cancellation reversal
* Ownership protection
* Invalid withdrawal state
* Not-found handling
* Idempotency
* Idempotency conflict
* Concurrent withdrawal protection
* Audit behavior

---

# 34. Manual API Verification Status

The backend has also been manually verified through Swagger/API calls using an ADMIN login.

Verified:

```text
Authentication / JWT
        ↓
GET Wallet
        ↓
Wallet Credit
        ↓
GET Payout Configuration
        ↓
POST Withdrawal
        ↓
Wallet Deduction
        ↓
Wallet Transactions
        ↓
Withdrawal History
        ↓
PENDING
        ↓
PROCESSING
        ↓
APPROVED
```

A separate rejection test verified:

```text
POST Withdrawal
        ↓
VES Deduction
        ↓
PENDING
        ↓
REJECTED
        ↓
VES Reversal
        ↓
CORRECTION Ledger
        ↓
Final Wallet Balance Restored
```

Database audit records were manually inspected for both flows.

---

# 35. Current Requirement Coverage — Implemented Backend Only

This section intentionally does **not** mark untouched future modules as failures.

## Complete

```text
Backend-driven wallet
Wallet database
VES
SVES
Gems
Tokens
Spins
Wallet ledger
Transaction types
Credit
Debit
Balance validation
Atomic wallet operations
Optimistic locking
Wallet APIs
Wallet service architecture
Transaction history
Pagination
Wallet summary
Payout methods
Payout options
Backend payout configuration
Backend payout calculation
Withdrawal model
Withdrawal creation
Withdrawal deduction
Withdrawal ledger
Withdrawal lifecycle
Approval
Rejection
Cancellation
Reversal
Ownership protection
Authentication
JWT
Authorization
Idempotency
Request fingerprinting
Concurrent withdrawal protection
Generic audit log
Withdrawal audit
Wallet audit
Admin withdrawal actions
Error handling
Data consistency
Automated regression tests
Manual API E2E verification
```

## Partially complete / future hardening

These are existing areas that may receive additional implementation later:

```text
Method-specific payout-detail validation
Advanced withdrawal eligibility rules
Extended security hardening
Fraud/risk checks
Audit context such as IP/session information where appropriate
```

These are **not treated as failures of the completed core system**. They are remaining hardening work.

---

# 36. Future Work — Not Yet Started / Not Part of Current Checkpoint

The following areas have deliberately not been used to invalidate the current core backend checkpoint:

```text
Rate limiting
Advanced fraud detection
Advanced eligibility engine
React frontend
API_DOCUMENTATION.md
Postman collection
Final README packaging
Deployment
Live frontend/backend
Final demonstration video
Final submission packaging
Scalability documentation
Advanced monitoring
Reconciliation infrastructure
Queue-based processing
Payout configuration administration
```

When these modules are implemented, their requirements should be moved from future/partial status to completed status after verification.

---

# 37. Security Status

## Already implemented

```text
JWT authentication                  ✅
Protected APIs                      ✅
Role-based authorization            ✅
User-level authorization            ✅
Admin wallet authorization          ✅
Server-side validation              ✅
Balance validation                  ✅
Idempotency                         ✅
Concurrent withdrawal protection   ✅
Audit logging                       ✅
Ownership protection                ✅
```

## Remaining security hardening

```text
Rate limiting                       ⏳
Advanced fraud/risk checks          ⏳
Advanced eligibility rules          ⏳
Method-specific payout validation   ⏳
Additional audit context            ⏳
```

---

# 38. Frontend Status

Frontend is intentionally **not part of the current backend checkpoint**.

Planned routes:

```text
/wallet
/payout
```

Optional:

```text
/login
```

The frontend must consume backend data and must not contain financial business logic.

Frontend implementation will be handled in a later phase.

---

# 39. API Documentation Status

Swagger/OpenAPI is already configured.

Major current APIs include:

```http
GET  /api/wallet
GET  /api/wallet/transactions
GET  /api/wallet/summary

POST /api/wallet/credit
POST /api/wallet/debit

GET  /api/payouts/configuration

POST /api/withdrawals
GET  /api/withdrawals
GET  /api/withdrawals/{withdrawalId}

PATCH /api/withdrawals/{withdrawalId}/processing
PATCH /api/withdrawals/{withdrawalId}/approve
PATCH /api/withdrawals/{withdrawalId}/reject
PATCH /api/withdrawals/{withdrawalId}/cancel
```

A separate:

```text
API_DOCUMENTATION.md
```

will be finalized during the documentation phase.

---

# 40. Environment and Production Safety

The project must not use VELoop production credentials or production databases.

Development architecture:

```text
Developer Frontend
       ↓
Developer Backend
       ↓
Developer MySQL
```

Never commit:

```text
.env
real passwords
database credentials
JWT secrets
API keys
production credentials
```

`.env.example` should contain configuration placeholders only.

---

# 41. Scalability

The final architecture documentation should address scaling from:

```text
1,000 users
        ↓
100,000 users
        ↓
1,000,000 users
```

Topics to address:

* Database transactions
* Atomic updates
* Ledger architecture
* Idempotency
* Indexing
* Queues
* Caching
* Rate limiting
* Fraud detection
* Audit logs
* Reconciliation
* Monitoring
* Horizontal scaling

This is a future documentation/hardening requirement and does not invalidate the current core backend checkpoint.

---

# 42. Development Roadmap

```text
PHASE 1
Project Setup
        ↓
PHASE 2
Database + Flyway
        ↓
PHASE 3
Authentication + JWT
        ↓
PHASE 3.5
OpenAPI / Swagger
        ↓
PHASE 4
Wallet Core
        ↓
V1-WALLET-CORE-COMPLETE
        ↓
PHASE 5
Payout Configuration
        ↓
V2-PAYOUT-CONFIGURATION-COMPLETE
        ↓
PHASE 6
Withdrawal System
        ↓
V3-WITHDRAWAL-SYSTEM-COMPLETE
        ↓
PHASE 7
Idempotency + Withdrawal Concurrency
        ↓
V4-IDEMPOTENCY-CONCURRENCY-COMPLETE
        ↓
PHASE 8
Audit Logging
        ↓
BACKEND-CHECKPOINT-01
        ↓
NEXT
Backend Security Hardening
        ↓
Future
Frontend
        ↓
Future
Documentation / Postman / Final Testing
        ↓
Future
Deployment / Demonstration
```

---

# 43. IMPORTANT — Current Resume Checkpoint

## `BACKEND-CHECKPOINT-01`

### Current verified state

```text
CORE WALLET + PAYOUT + WITHDRAWAL
+ IDEMPOTENCY
+ CONCURRENCY
+ AUDIT
```

### Automated baseline

```text
56/56 PASS
BUILD SUCCESS
```

### Manual baseline

```text
ADMIN JWT API E2E VERIFIED
```

### Completed core modules

```text
Authentication                  ✅
JWT                            ✅
Authorization                 ✅
OpenAPI / Swagger              ✅

Wallet                         ✅
Wallet Ledger                  ✅
Wallet Transactions            ✅
Wallet Pagination              ✅
Wallet Summary                 ✅
Wallet Validation              ✅
Wallet Atomicity               ✅
Wallet Optimistic Locking      ✅
Wallet Concurrency             ✅
Wallet User Isolation          ✅

Payout Configuration           ✅
Payout Methods                 ✅
Payout Options                 ✅
Payout Configuration API       ✅

Withdrawal Creation            ✅
Withdrawal Deduction           ✅
Withdrawal Ledger              ✅
Withdrawal Processing          ✅
Withdrawal Approval            ✅
Withdrawal Rejection           ✅
Withdrawal Reversal            ✅
Withdrawal Cancellation        ✅
Withdrawal Ownership           ✅
Withdrawal State Validation    ✅

Idempotency                    ✅
Request Fingerprinting        ✅
Withdrawal Concurrency        ✅

Generic Audit Log              ✅
Withdrawal Audit               ✅
Wallet Audit                   ✅
Admin Actor Tracking           ✅
```

---

# 44. Important Business Rules Already Implemented

## Payout Mapping

```text
payoutAmount
    =
actual INR payout value

currencyAmount
    =
required VES
```

Example:

```text
₹10 INR
requires
2400 VES
```

## Withdrawal Deduction

```text
Withdrawal creation
        ↓
Immediate VES deduction
        ↓
WITHDRAWAL ledger transaction
        ↓
PENDING withdrawal
```

## Rejection

```text
PENDING / PROCESSING
        ↓
REJECTED
        ↓
VES reversal
        ↓
CORRECTION ledger transaction
```

## Cancellation

```text
PENDING
        ↓
CANCELLED
        ↓
VES reversal
        ↓
CORRECTION ledger transaction
```

## Approval

```text
PENDING / PROCESSING
        ↓
APPROVED
```

Approval does **not** deduct VES a second time.

## Idempotency

```text
Same user
+
Same idempotency key
+
Same request
        ↓
Existing withdrawal reused
```

Same key with a different request:

```text
409 Conflict
```

## Concurrency

Concurrent wallet modifications are protected using optimistic locking and transactional wallet operations.

## Ownership

```text
Authenticated user
        ↓
Only own wallet/withdrawals accessible
```

## Admin operations

```text
PROCESSING
APPROVE
REJECT
        ↓
ADMIN only
```

## User cancellation

```text
CANCEL
        ↓
Authenticated owner
        ↓
PENDING only
```

---

# 45. Current Test / Verification Checklist

```text
Wallet credit                         ✅
Wallet debit                          ✅
Insufficient balance                  ✅
Wallet ledger                         ✅
Wallet transaction history            ✅
Wallet summary                        ✅
User isolation                        ✅
JWT isolation                         ✅
Admin wallet authorization            ✅
Payout configuration                  ✅
Invalid payout method                 ✅
Invalid payout option                 ✅
Payout option/method mismatch         ✅
Normal withdrawal                     ✅
Withdrawal deduction                  ✅
Withdrawal ledger                     ✅
Processing                            ✅
Approval                              ✅
Rejection                             ✅
Rejection reversal                    ✅
Cancellation                          ✅
Cancellation reversal                 ✅
Ownership protection                  ✅
Invalid withdrawal state              ✅
Not-found handling                    ✅
Idempotency                           ✅
Idempotency conflict                  ✅
Concurrent withdrawal protection      ✅
Generic audit                         ✅
Withdrawal audit                      ✅
Manual approved E2E                   ✅
Manual rejected/reversal E2E          ✅
```

Current baseline:

```text
56/56 PASS
```

---

# 46. What NOT To Rebuild

When returning to this project, do **not** restart from:

```text
Project Setup
Database
Authentication
JWT
Wallet
Wallet Ledger
Wallet Concurrency
Payout Configuration
Basic Withdrawal
Withdrawal Deduction
Withdrawal Reversal
Withdrawal Cancellation
Idempotency
Withdrawal Concurrency
Audit Logging
```

These are verified baselines.

Only change them if a future requirement explicitly requires an extension, correction, or hardening.

---

# 47. Exact Next Starting Point

When development resumes:

### Step 1

Run:

```bash
mvn clean test
```

Confirm:

```text
56/56 PASS
BUILD SUCCESS
```

### Step 2

Do **not** redo the completed E2E withdrawal flow unless regression testing requires it.

### Step 3

Continue with:

```text
BACKEND SECURITY HARDENING
```

The first remaining backend areas are:

```text
1. Rate limiting
2. Method-specific payout-detail validation
3. Withdrawal eligibility hardening
4. Fraud/risk/security checks
5. Additional security verification
```

These should be implemented incrementally.

### Step 4

After each new security change:

```text
Implement
   ↓
Add/update tests
   ↓
mvn clean test
   ↓
Verify API behavior
   ↓
Update README
```

### Step 5

Create the next checkpoint only after the new work is verified.

---

# 48. Future Checkpoint Format

Every future checkpoint should record:

```text
Checkpoint Name
Phase
Implemented Modules
Database Migrations
Important Business Rules
Automated Test Count
Manual API Verification
Known Partial Areas
Untouched Future Areas
Exact Next Starting Point
```

This prevents having to inspect the entire repository every time development resumes.

---

# 49. Final Current Checkpoint

```text
========================================================

BACKEND-CHECKPOINT-01

CORE WALLET + PAYOUT + WITHDRAWAL
+ IDEMPOTENCY + CONCURRENCY + AUDIT

========================================================

Automated Tests:
56/56 PASS

Manual API Verification:
PASS

Admin JWT Verification:
PASS

Wallet:
COMPLETE + VERIFIED

Ledger:
COMPLETE + VERIFIED

Payout Configuration:
COMPLETE + VERIFIED

Withdrawal:
COMPLETE + VERIFIED

Reversal:
COMPLETE + VERIFIED

Idempotency:
COMPLETE + VERIFIED

Concurrency:
COMPLETE + VERIFIED

Audit:
COMPLETE + VERIFIED

========================================================

NEXT DEVELOPMENT AREA:

BACKEND SECURITY HARDENING

========================================================

DO NOT REBUILD PREVIOUS MODULES.

Resume from this checkpoint.

========================================================
```

**This README is now the authoritative development resume point.**
When you return to the project, the first thing to do is read this checkpoint and run `mvn clean test`; there is no need to reconstruct the previous development history from scratch.
