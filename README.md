# VELoop Rewards — Wallet & Withdrawal Backend

> **Development Status:** IN PROGRESS
>
> A secure, backend-driven Wallet & Withdrawal system for VELoop Rewards, built incrementally with phase-based development checkpoints.
>
> **Current Checkpoint:** `V3-WITHDRAWAL-SYSTEM-COMPLETE`
>
> **Current Phase:** Phase 6 — Withdrawal System **COMPLETED AND VERIFIED**
>
> **Next Phase:** Phase 7 — Idempotency + Withdrawal Concurrency

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

The project is an **independent development/demo environment**. It does not connect to VELoop production systems or production databases.

The backend is always the **single source of truth**.

The frontend must never be able to directly modify wallet balances.

For example, changing:

```text
VES: 1000 → 100000
```

in browser storage or frontend JavaScript must never change the actual wallet balance stored by the backend.

---

# 2. Project Objective

The final system follows this high-level flow:

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
 ├── Wallet deduction
 └── Ledger transaction
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

Idempotency and advanced withdrawal concurrency protections are implemented/planned separately in **Phase 7**.

Every financially important operation must be controlled by the backend.

---

# 3. Core Design Principle — Backend Is the Source of Truth

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

The frontend must not be able to send an arbitrary amount such as:

```json
{
  "amount": 19500
}
```

and force the backend to trust that value.

---

# 4. Current Development Status

## Completed

### Project Foundation

* Java 21
* Spring Boot 3.5.16
* Maven
* MySQL 8.x
* Spring Data JPA
* Hibernate
* Flyway
* Spring Security
* JWT authentication
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
* VEs
* SVEs
* Gems
* Tokens
* Spins
* Wallet retrieval
* Automatic wallet creation after registration
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
* Atomic wallet + ledger operations
* Concurrent balance protection
* Database indexing
* Integration test suite

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
* Payout configuration REST API
* Swagger/OpenAPI documentation
* Payout configuration integration tests

### Withdrawal System — Phase 6

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
* Withdrawal integration tests

---

# 5. Current Checkpoint

## `V3-WITHDRAWAL-SYSTEM-COMPLETE`

The project has successfully completed:

```text
Phase 1 — Project Setup
        ↓
Phase 2 — Database + Flyway
        ↓
Phase 3 — Authentication + JWT
        ↓
Phase 3.5 — OpenAPI / Swagger
        ↓
Phase 4 — Wallet Core
        ↓
V1-WALLET-CORE-COMPLETE
        ↓
Phase 5 — Payout Configuration
        ↓
V2-PAYOUT-CONFIGURATION-COMPLETE
        ↓
Phase 6 — Withdrawal System
        ↓
V3-WITHDRAWAL-SYSTEM-COMPLETE
        ↓
52/52 TESTS PASSING
        ↓
NEXT: PHASE 7
```

---

# 6. Checkpoint Verification

| Area                            | Status              |
| ------------------------------- | ------------------- |
| Project setup                   | ✅ Completed         |
| Java 21                         | ✅ Verified          |
| Spring Boot 3.5.16              | ✅ Verified          |
| Maven build                     | ✅ Verified          |
| MySQL                           | ✅ Completed         |
| Flyway                          | ✅ Completed         |
| Users migration                 | ✅ Completed         |
| User entity                     | ✅ Completed         |
| Registration                    | ✅ Completed         |
| Login                           | ✅ Completed         |
| JWT                             | ✅ Completed         |
| JWT filter                      | ✅ Completed         |
| SecurityContext                 | ✅ Completed         |
| Authentication                  | ✅ Completed         |
| Authorization                   | ✅ Completed         |
| Global exception handling       | ✅ Completed         |
| OpenAPI                         | ✅ Completed         |
| Swagger UI                      | ✅ Completed         |
| Wallet migration                | ✅ Completed         |
| Wallet entity                   | ✅ Completed         |
| Wallet repository               | ✅ Completed         |
| Automatic wallet creation       | ✅ Completed         |
| Wallet retrieval                | ✅ Completed         |
| Wallet credit                   | ✅ Completed         |
| Wallet debit                    | ✅ Completed         |
| Balance validation              | ✅ Completed         |
| Insufficient balance protection | ✅ Completed         |
| Multi-currency support          | ✅ Completed         |
| Wallet ledger                   | ✅ Completed         |
| Transaction history             | ✅ Completed         |
| Pagination                      | ✅ Completed         |
| Wallet summary                  | ✅ Completed         |
| User isolation                  | ✅ Verified          |
| JWT wallet isolation            | ✅ Verified          |
| Admin wallet authorization      | ✅ Verified          |
| Atomicity                       | ✅ Verified          |
| Optimistic locking              | ✅ Verified          |
| Concurrent debit protection     | ✅ Verified          |
| Ledger consistency              | ✅ Verified          |
| Payout methods                  | ✅ Completed         |
| Payout options                  | ✅ Completed         |
| Payout configuration service    | ✅ Completed         |
| Payout configuration API        | ✅ Completed         |
| Payout API authentication       | ✅ Verified          |
| Payout configuration tests      | ✅ Verified          |
| Withdrawal migration            | ✅ Completed         |
| Withdrawal entity               | ✅ Completed         |
| Withdrawal repository           | ✅ Completed         |
| Withdrawal service              | ✅ Completed         |
| Withdrawal API                  | ✅ Completed         |
| Wallet deduction integration    | ✅ Verified          |
| Withdrawal ledger integration   | ✅ Verified          |
| Withdrawal approval             | ✅ Verified          |
| Withdrawal rejection            | ✅ Verified          |
| Rejection balance reversal      | ✅ Verified          |
| Withdrawal cancellation         | ✅ Verified          |
| Cancellation balance reversal   | ✅ Verified          |
| Withdrawal ownership protection | ✅ Verified          |
| Withdrawal state validation     | ✅ Verified          |
| Withdrawal not-found handling   | ✅ Verified          |
| Withdrawal integration tests    | ✅ 14/14             |
| Full regression suite           | ✅ **52/52 passing** |

---

# 7. Technology Stack

## Backend

* Java 21
* Spring Boot 3.5.16
* Maven
* Spring Web
* Spring Data JPA
* Hibernate
* Spring Security
* JJWT 0.12.6
* Jakarta Bean Validation
* Flyway
* Springdoc OpenAPI
* Swagger UI

## Database

* MySQL 8.x

## Testing

* JUnit 5
* Mockito
* Spring Boot Test
* MockMvc
* Integration testing
* Testcontainers — planned for further infrastructure hardening
* Postman — planned
* Swagger UI

## Frontend

Planned:

* React
* Vite
* Bootstrap / CSS

---

# 8. Current Architecture

```text
                    React + Vite
                         │
                         │ REST + JWT
                         ▼
               ┌─────────────────────┐
               │   Spring Boot API   │
               └──────────┬──────────┘
                          │
             ┌────────────┼────────────┐
             │            │            │
             ▼            ▼            ▼
           Auth         Wallet       Payout
             │            │            │
             ▼            ▼            ▼
           User         Ledger     Configuration
                          │            │
                          └─────┬──────┘
                                │
                                ▼
                         Withdrawal
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

# 9. Current Package Architecture

```text
src/main/java/com/veloop/rewards/

│
├── config/
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   └── RateLimitConfig.java
│
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   ├── CustomUserDetailsService.java
│   └── SecurityExceptionHandler.java
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
│   │   └── PayoutConfigurationController.java
│   ├── service/
│   │   └── PayoutConfigurationService.java
│   ├── repository/
│   │   ├── PayoutMethodRepository.java
│   │   └── PayoutOptionRepository.java
│   ├── entity/
│   │   ├── PayoutMethod.java
│   │   └── PayoutOption.java
│   └── dto/
│       ├── PayoutMethodResponse.java
│       └── PayoutOptionResponse.java
│
├── withdrawal/
│   ├── controller/
│   │   └── WithdrawalController.java
│   ├── service/
│   │   └── WithdrawalService.java
│   ├── repository/
│   │   └── WithdrawalRepository.java
│   ├── entity/
│   │   └── Withdrawal.java
│   ├── dto/
│   │   ├── WithdrawalCreateRequest.java
│   │   └── WithdrawalResponse.java
│   └── enums/
│       └── WithdrawalStatus.java
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

# 10. Database Migrations

Current migrations:

```text
src/main/resources/db/migration/

├── V1__create_users.sql
├── V2__create_wallets.sql
├── V3__create_wallet_transactions.sql
├── V4__create_payout_configuration.sql
└── V5__create_withdrawals.sql
```

Current migration responsibility:

```text
V1 → Users
V2 → Wallets
V3 → Wallet Transactions / Ledger
V4 → Payout Methods + Payout Options
V5 → Withdrawals
```

Future migrations:

```text
V6+ → Idempotency
V6+ → Audit
V6+ → Additional security/infrastructure requirements
```

Do not modify already-applied migrations unless there is a deliberate migration strategy. New schema changes should normally use a new Flyway migration.

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

The wallet database contains separate balances for every currency.

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

`BigDecimal` is used for monetary/reward values rather than floating-point types.

---

# 12. Wallet Database Design

Migration:

```text
V2__create_wallets.sql
```

Table:

```text
wallets
```

Important constraints:

```text
user_id UNIQUE

user_id → users.id
```

Therefore:

```text
One User
   │
   └── One Wallet
```

Optimistic locking is implemented through:

```java
@Version
private Long version;
```

This protects wallet updates against concurrent modifications.

---

# 13. Wallet Ledger

Migration:

```text
V3__create_wallet_transactions.sql
```

Table:

```text
wallet_transactions
```

The ledger stores:

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

## Debit

```text
WITHDRAWAL
EXCHANGE_DEBIT
ADMIN_DEBIT
CORRECTION
```

These types allow the wallet to later support:

* Ads
* Referrals
* Daily rewards
* Games
* Spins
* Exchanges
* Withdrawals
* Administrative operations

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

Wallet mutations:

```http
POST /api/wallet/credit
POST /api/wallet/debit
```

are restricted to:

```text
ADMIN
```

Normal users receive:

```text
403 Forbidden
```

---

# 16. Payout Configuration

The payout configuration is backend-controlled.

Database relationship:

```text
payout_methods
        │
        │ 1 : N
        ▼
payout_options
```

## Payout Method

```text
PayoutMethod

├── id
├── code
├── name
├── active
├── createdAt
└── updatedAt
```

## Payout Option

```text
PayoutOption

├── id
├── method
├── payoutAmount
├── currency
├── currencyAmount
├── active
├── createdAt
└── updatedAt
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

| Payout Amount | Currency | Required VES |
| ------------: | :------: | -----------: |
|           ₹10 |    INR   |        2,400 |
|           ₹25 |    INR   |        5,800 |
|           ₹50 |    INR   |       10,000 |
|          ₹100 |    INR   |       19,500 |
|          ₹150 |    INR   |       28,500 |
|          ₹300 |    INR   |       52,500 |
|          ₹500 |    INR   |       80,500 |
|        ₹1,000 |    INR   |      150,000 |

Important implementation mapping:

```text
PayoutOption.payoutAmount
    ↓
Actual cash/gift-card value

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

Authentication:

```text
Bearer JWT
```

The API returns:

* Active payout methods
* Active payout options
* Payout amount
* Currency
* Required VES

Inactive payout methods/options are excluded.

The endpoint is read-only for normal API consumers.

---

# 19. Phase 6 — Withdrawal System

## Status

```text
COMPLETED AND VERIFIED
```

Migration:

```text
V5__create_withdrawals.sql
```

Table:

```text
withdrawals
```

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
                 ┌── APPROVED
                 │
PENDING → PROCESSING
                 │
                 └── REJECTED
```

Additionally:

```text
PENDING → CANCELLED
```

User cancellation is allowed only while the withdrawal is `PENDING`.

Admin processing/approval/rejection operations are protected with admin authorization.

---

# 21. Withdrawal APIs

## Create Withdrawal

```http
POST /api/withdrawals
```

Authenticated user only.

Request:

```json
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "test@upi"
}
```

The backend resolves:

```text
Payout Method
Payout Option
Payout Amount
Required VES
Currency
Active status
Method/Option relationship
```

The client does not provide the financial amount to deduct.

---

## Withdrawal History

```http
GET /api/withdrawals?page=1&limit=20
```

Returns only withdrawals belonging to the authenticated user.

---

## Withdrawal Details

```http
GET /api/withdrawals/{withdrawalId}
```

Returns the requested withdrawal only if it belongs to the authenticated user.

---

## Admin — Processing

```http
PATCH /api/withdrawals/{withdrawalId}/processing
```

Admin only.

Allowed transition:

```text
PENDING → PROCESSING
```

---

## Admin — Approval

```http
PATCH /api/withdrawals/{withdrawalId}/approve
```

Admin only.

Allowed transitions:

```text
PENDING → APPROVED
PROCESSING → APPROVED
```

Approval does not perform another wallet deduction.

---

## Admin — Rejection

```http
PATCH /api/withdrawals/{withdrawalId}/reject
```

Admin only.

A rejection requires a rejection reason.

Allowed transitions:

```text
PENDING → REJECTED
PROCESSING → REJECTED
```

Rejection reverses the original wallet deduction.

---

## User — Cancellation

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
```

User only.

Allowed transition:

```text
PENDING → CANCELLED
```

Cancellation reverses the original wallet deduction.

---

# 22. Withdrawal Wallet Deduction Strategy

The implemented strategy is:

> **Immediate VES deduction at withdrawal creation.**

Example:

```text
Initial balance
10,000 VES

Withdrawal
2,400 VES

Remaining balance
7,600 VES
```

Withdrawal creation performs:

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
```

The wallet deduction and withdrawal creation occur inside the transactional service flow.

---

# 23. Withdrawal Ledger Transaction

For a withdrawal:

```text
Transaction Type:
WITHDRAWAL

Currency:
VES

Amount:
Required VES

Source:
WITHDRAWAL

Reference ID:
Withdrawal ID

Description:
Wallet withdrawal
```

Example:

```text
Initial Balance: 10,000 VES
Withdrawal:       2,400 VES
Final Balance:    7,600 VES
```

The withdrawal stores a reference to the corresponding wallet transaction.

---

# 24. Withdrawal Rejection Reversal

When a withdrawal is rejected:

```text
Withdrawal
    ↓
REJECTED
    ↓
Reverse original VES deduction
    ↓
Create CORRECTION ledger transaction
```

The reversal uses:

```text
Transaction Type:
CORRECTION

Source:
WITHDRAWAL_REJECTED

Reference:
<withdrawalId>-REVERSAL
```

Example:

```text
Before withdrawal:
10,000 VES

Withdrawal:
-2,400 VES

Balance:
7,600 VES

Rejected

Reversal:
+2,400 VES

Final:
10,000 VES
```

This behavior has been integration-tested.

---

# 25. Withdrawal Cancellation Reversal

When a user cancels a `PENDING` withdrawal:

```text
PENDING
   ↓
CANCELLED
   ↓
Reverse VES deduction
   ↓
Create CORRECTION ledger transaction
```

The reversal uses:

```text
Transaction Type:
CORRECTION

Source:
WITHDRAWAL_CANCELLED

Reference:
<withdrawalId>-CANCELLATION-REVERSAL
```

This behavior has been integration-tested.

---

# 26. Withdrawal Validation

The backend validates:

* User existence
* Payout method existence
* Payout method active status
* Payout option existence
* Payout option active status
* Payout option belongs to selected payout method
* Wallet availability
* Required VES balance
* Withdrawal ownership
* Withdrawal lifecycle state
* Required rejection reason

The frontend cannot override these checks.

---

# 27. Withdrawal Security / Ownership

A withdrawal is always associated with a specific user.

For:

```http
GET /api/withdrawals/{withdrawalId}
```

and:

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
```

the backend verifies that the authenticated user owns the withdrawal.

Another user cannot access or cancel it.

Admin-only lifecycle operations are protected using:

```text
ROLE_ADMIN
```

---

# 28. Withdrawal Exception Handling

The withdrawal implementation uses dedicated business exceptions where implemented:

```text
InvalidWithdrawalRequestException
InvalidWithdrawalStateException
WithdrawalNotFoundException
WithdrawalOwnershipException
InsufficientBalanceException
```

Generic validation and business errors are handled through the global exception handler.

Important verified cases include:

```text
Invalid payout configuration
        ↓
400-level business error

Invalid withdrawal state
        ↓
400-level business error

Withdrawal not found
        ↓
404

Wrong user / ownership violation
        ↓
403

Insufficient VES
        ↓
400-level business error
```

---

# 29. Withdrawal Test Coverage

Integration test class:

```text
src/test/java/com/veloop/rewards/withdrawal/WithdrawalServiceIntegrationTest.java
```

Current result:

```text
WithdrawalServiceIntegrationTest

14/14 PASS
```

Tests cover:

```text
1. shouldCreateWithdrawalAndDeductVes
2. shouldCreateWithdrawalLedgerTransaction
3. shouldRejectWithdrawalWhenBalanceIsInsufficient
4. shouldRejectInactivePayoutMethod
5. shouldRejectInactivePayoutOption
6. shouldRejectPayoutOptionFromDifferentMethod
7. shouldMoveWithdrawalToProcessing
8. shouldApproveWithdrawal
9. shouldRejectWithdrawalAndReverseVes
10. shouldCancelWithdrawalAndReverseVes
11. shouldRejectInvalidWithdrawalState
12. shouldRejectWithdrawalFromAnotherUser
13. shouldThrowNotFoundForInvalidWithdrawalId
14. shouldNotRejectApprovedWithdrawal
```

Verified scenarios include:

* Normal withdrawal
* Wallet deduction
* Withdrawal ledger creation
* Insufficient balance
* Inactive payout method
* Inactive payout option
* Payout method/option mismatch
* Processing
* Approval
* Rejection
* Rejection reversal
* Cancellation
* Cancellation reversal
* Invalid lifecycle transition
* User ownership isolation
* Invalid withdrawal ID

---

# 30. Current Full Test Status

The complete regression suite currently passes:

```text
Tests run: 52
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Current verified baseline:

```text
Wallet Core
    PASS

Payout Configuration
    PASS

Withdrawal System
    14/14 PASS

--------------------------------

FULL SUITE
52/52 PASS
```

This is the **current verified baseline** for future development.

Before starting a future phase, run:

```cmd
mvnw clean test
```

Expected current result:

```text
Tests run: 52
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

---

# 31. Phase 7 — Idempotency + Withdrawal Concurrency

## NEXT PHASE

The next implementation phase is:

```text
PHASE 7 — IDEMPOTENCY + WITHDRAWAL CONCURRENCY
```

The basic withdrawal flow is already complete.

**Do not rebuild Phase 6.**

Phase 7 should focus only on hardening the existing withdrawal flow.

---

# 32. Phase 7 — Idempotency

The withdrawal system must prevent duplicate requests.

Example:

```text
User clicks Redeem
       ↓
Request 1

Network retry / double click
       ↓
Request 2
```

The system must not create:

```text
2 withdrawals
2 wallet deductions
```

for the same idempotent request.

Phase 7 should introduce:

```text
Idempotency key
        ↓
Existing request lookup
        ↓
Return existing result
```

The exact implementation should be designed against the existing withdrawal flow rather than rebuilding withdrawal creation.

Expected future components:

```text
idempotency/
├── entity/
├── repository/
└── service/
```

A Flyway migration will be added when the database design is finalized.

---

# 33. Phase 7 — Withdrawal Concurrency

The withdrawal flow must safely handle concurrent withdrawal requests.

Example:

```text
Available balance = 10,000 VES

Request A → 8,000 VES
Request B → 8,000 VES
```

The final state must not allow:

```text
Balance < 0
```

or:

```text
Successful deductions:
A = 8,000
B = 8,000
```

when only 10,000 VES were available.

The existing wallet optimistic-locking and transactional behavior should be reused and extended rather than replaced unnecessarily.

Phase 7 must add dedicated withdrawal concurrency tests.

---

# 34. Phase 7 Expected Tests

At minimum, Phase 7 should verify:

```text
1. Duplicate withdrawal request
2. Same idempotency key repeated
3. Different idempotency keys
4. Concurrent withdrawals against same wallet
5. Insufficient balance under concurrency
6. No duplicate wallet deduction
7. No duplicate withdrawal record
8. Ledger consistency
9. Safe retry behavior
```

The existing Phase 6 tests must continue passing.

Expected rule:

```text
Before Phase 7:
52/52 PASS

After Phase 7:
All previous tests PASS
+
New Phase 7 tests PASS
```

---

# 35. Phase 7 Implementation Rule

Do not start by rewriting existing:

```text
WalletService
PayoutConfigurationService
WithdrawalService
WithdrawalController
```

unless a specific Phase 7 requirement requires a change.

First understand and extend the existing implementation.

The following are considered **frozen working baselines**:

```text
Wallet Core
Payout Configuration
Basic Withdrawal Creation
Withdrawal Deduction
Withdrawal Ledger
Withdrawal Approval
Withdrawal Rejection
Withdrawal Reversal
Withdrawal Cancellation
```

---

# 36. Audit Logging

Audit logging is planned for Phase 8.

Important events:

```text
Withdrawal Created
Withdrawal Approved
Withdrawal Rejected
Withdrawal Cancelled
Wallet Credit
Wallet Debit
Balance Correction
Payout Configuration Changed
```

Planned fields:

```text
actorId
action
targetUserId
targetType
referenceId
metadata
IP/session information where appropriate
createdAt
```

Do not implement the complete audit system as part of Phase 7 unless a Phase 7 dependency requires it.

---

# 37. Rate Limiting

Rate limiting belongs to the security-hardening phase.

Sensitive APIs include:

* Authentication
* Wallet mutations
* Withdrawal
* OTP/authentication-related endpoints

Current status:

```text
Planned / incomplete
```

---

# 38. Fraud & Security Checks

Future withdrawal processing should support:

* Account status checks
* Eligibility checks
* Duplicate request prevention
* Suspicious activity handling
* Server-side validation
* Payout validation
* Withdrawal validation
* Audit logging
* Reconciliation

These controls will be introduced incrementally.

---

# 39. Planned Frontend

A small React/Vite frontend will demonstrate the backend.

Required routes:

```text
/wallet
/payout
```

Optional:

```text
/login
```

## Wallet

The frontend should:

* Fetch wallet from backend
* Display balances
* Display transactions
* Provide withdrawal/redeem action
* Navigate to payout
* Show loading state
* Show error state
* Refresh after successful operations

## Payout

```text
Fetch methods
      ↓
Select method
      ↓
Select option
      ↓
Enter details
      ↓
Confirmation
      ↓
POST withdrawal
```

The frontend must not contain financial business logic.

---

# 40. API Documentation

The project will contain:

```text
API_DOCUMENTATION.md
```

It will document:

* Endpoint
* HTTP method
* Authentication
* Request
* Response
* Errors
* Examples

Current major APIs:

```text
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

Swagger/OpenAPI is already configured.

---

# 41. Environment & Security

Sensitive values must never be committed.

Use:

```text
.env.example
```

for configuration examples.

Never commit:

```text
.env
real passwords
database credentials
JWT secrets
API keys
production credentials
```

The development system must use its own local/development database.

---

# 42. Production Access Restriction

This project must not connect to VELoop production databases or production services.

Development architecture:

```text
Developer Frontend
       ↓
Developer Backend
       ↓
Developer MySQL
```

Production integration, if required, must be performed later by authorized VELoop developers.

---

# 43. Scalability Requirement

The final architecture discussion should answer:

> If VELoop Rewards grows from 1,000 users to 1,000,000 users, what changes would be required?

The final discussion should cover:

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
* Scalability

---

# 44. Final Testing Requirements

The final system must demonstrate at least:

```text
1. Normal credit
2. Normal withdrawal
3. Insufficient balance
4. Duplicate withdrawal
5. Concurrent withdrawals
6. Invalid payout option
7. Another user's wallet
8. Rejected withdrawal reversal
9. Frontend/API manipulation
```

Current verified:

```text
Normal credit                         ✅
Normal withdrawal                     ✅
Insufficient balance                  ✅
Concurrent wallet updates             ✅
Another user's wallet                 ✅
Wallet/API manipulation protection    ✅
Rejected withdrawal reversal          ✅
Invalid payout option                 ✅
```

Still specifically requiring Phase 7:

```text
Duplicate withdrawal / idempotency    ⏳
Withdrawal concurrency                ⏳
```

---

# 45. Development Roadmap

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
PHASE 8
Audit + Security Hardening
        ↓
PHASE 9
React Demonstration Frontend
        ↓
PHASE 10
API Documentation + Postman + Final Testing
        ↓
PHASE 11
Deployment / Demonstration
```

---

# 46. IMPORTANT — Continuation Checkpoint

This section is intentionally detailed so future development can resume without inspecting the entire repository.

## Current checkpoint

```text
V3-WITHDRAWAL-SYSTEM-COMPLETE
```

## Current verified test baseline

```text
52/52 PASS
BUILD SUCCESS
```

## Completed modules

```text
Authentication
        ✅

JWT
        ✅

Authorization
        ✅

OpenAPI / Swagger
        ✅

Wallet
        ✅

Wallet Ledger
        ✅

Wallet Transactions
        ✅

Wallet Pagination
        ✅

Wallet Summary
        ✅

Wallet Validation
        ✅

Wallet Atomicity
        ✅

Wallet Optimistic Locking
        ✅

Wallet Concurrency
        ✅

Wallet User Isolation
        ✅

Payout Configuration
        ✅

Payout Methods
        ✅

Payout Options
        ✅

Payout Configuration API
        ✅

Basic Withdrawal System
        ✅

Withdrawal Deduction
        ✅

Withdrawal Ledger
        ✅

Withdrawal Processing
        ✅

Withdrawal Approval
        ✅

Withdrawal Rejection
        ✅

Withdrawal Reversal
        ✅

Withdrawal Cancellation
        ✅

Withdrawal Ownership
        ✅

Withdrawal State Validation
        ✅

Withdrawal Integration Tests
        ✅ 14/14
```

## Important business rules already implemented

### Payout mapping

```text
payoutAmount
    = actual INR payout value

currencyAmount
    = required VES
```

Example:

```text
₹10 INR
requires
2400 VES
```

### Withdrawal deduction

```text
Withdrawal creation
        ↓
Immediate VES deduction
        ↓
WITHDRAWAL ledger transaction
        ↓
PENDING withdrawal
```

### Rejection

```text
PENDING/PROCESSING
        ↓
REJECTED
        ↓
VES reversal
        ↓
CORRECTION ledger transaction
```

### Cancellation

```text
PENDING
        ↓
CANCELLED
        ↓
VES reversal
        ↓
CORRECTION ledger transaction
```

### Approval

```text
PENDING/PROCESSING
        ↓
APPROVED
```

Approval does **not** deduct VES a second time.

### Ownership

```text
Authenticated user
        ↓
Only own withdrawals accessible
```

### Admin operations

```text
PROCESSING
APPROVE
REJECT

        ↓

ADMIN only
```

### User operation

```text
CANCEL

        ↓

Authenticated owner only
        ↓
PENDING only
```

---

# 47. Exact Next Starting Point

When continuing development, **DO NOT START FROM PROJECT SETUP**.

Do not rebuild:

```text
Authentication
JWT
Wallet
Wallet Ledger
Wallet Concurrency
Payout Configuration
Basic Withdrawal Flow
Withdrawal Deduction
Withdrawal Reversal
Withdrawal Cancellation
```

First run:

```cmd
mvnw clean test
```

Expected:

```text
Tests run: 52
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Then continue directly with:

```text
PHASE 7 — IDEMPOTENCY + WITHDRAWAL CONCURRENCY
```

### Phase 7 first task

Design the idempotency mechanism for:

```http
POST /api/withdrawals
```

Before writing code, establish:

```text
Idempotency key
        ↓
Database uniqueness
        ↓
Existing request lookup
        ↓
Safe retry behavior
        ↓
Interaction with wallet transaction
        ↓
Interaction with withdrawal transaction
```

Then implement and test it incrementally.

---

# 48. Expected Final Repository Structure

```text
veloop-rewards-backend/

│
├── pom.xml
├── README.md
├── API_DOCUMENTATION.md
├── .env.example
├── .gitignore
│
├── src/
│   ├── main/
│   │   ├── java/com/veloop/rewards/
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │
│   └── test/
│       └── java/com/veloop/rewards/
│
└── frontend/
    └── React/Vite application
```

---

# 49. Final Submission Checklist

## Backend

* [x] Wallet completely backend-driven
* [x] VES server-authoritative
* [x] SVES server-authoritative
* [x] Gems server-authoritative
* [x] Tokens server-authoritative
* [x] Spins server-authoritative
* [x] Wallet transactions stored
* [x] Credits create ledger records
* [x] Debits create ledger records
* [x] Withdrawal requests stored
* [x] Payout options come from backend
* [x] Payout values controlled by backend
* [x] Insufficient balance rejected
* [ ] Duplicate withdrawal prevented
* [x] Concurrent wallet balance updates handled safely
* [x] Users cannot access another user's wallet
* [x] Authentication implemented
* [x] Sensitive wallet APIs protected
* [x] Wallet validation implemented
* [ ] Rate limiting fully implemented
* [ ] Audit logging implemented
* [ ] Withdrawal idempotency
* [ ] Withdrawal advanced concurrency tests
* [ ] Withdrawal fraud/security hardening

## Payout Configuration

* [x] Payout method table
* [x] Payout option table
* [x] Payout method entity
* [x] Payout option entity
* [x] Payout repositories
* [x] Payout DTOs
* [x] Payout configuration service
* [x] Payout configuration API
* [x] Active/inactive filtering
* [x] Payout configuration tests

## Withdrawal

* [x] Withdrawal table
* [x] Withdrawal entity
* [x] Withdrawal repository
* [x] Withdrawal DTOs
* [x] Withdrawal service
* [x] Withdrawal API
* [x] Backend payout validation
* [x] Immediate VES deduction
* [x] Withdrawal ledger transaction
* [x] Approval workflow
* [x] Rejection workflow
* [x] Rejection reversal
* [x] Cancellation workflow
* [x] Cancellation reversal
* [x] Ownership validation
* [x] State validation
* [x] Withdrawal integration tests
* [ ] Idempotency
* [ ] Advanced concurrency hardening

## Frontend

* [ ] `/wallet`
* [ ] `/payout`
* [ ] Backend-connected wallet
* [ ] Backend-connected payout options
* [ ] Backend-connected withdrawal
* [ ] Loading states
* [ ] Error states
* [ ] Wallet refresh after withdrawal

## Documentation

* [x] README development status
* [x] Architecture documentation
* [x] Current API documentation through OpenAPI/Swagger
* [ ] API_DOCUMENTATION.md complete
* [x] Database/model documentation
* [x] Current test results documented
* [ ] Postman collection

## Security

* [x] `.env` excluded
* [x] `.env.example` provided
* [x] No production credentials
* [x] No production database connection
* [ ] Rate limiting
* [ ] Audit logging
* [ ] Withdrawal idempotency
* [ ] Withdrawal fraud/security checks

## Delivery

* [ ] GitHub repository
* [ ] Live frontend
* [ ] Live backend/API
* [ ] Demonstration/video
* [ ] Final test evidence

---

# 50. Current Final Checkpoint

```text
V3-WITHDRAWAL-SYSTEM-COMPLETE
```

Verified:

```text
Authentication
        ✅

JWT
        ✅

Authorization
        ✅

OpenAPI / Swagger
        ✅

Wallet
        ✅

Balances
        ✅

Ledger
        ✅

Transactions
        ✅

Pagination
        ✅

Summary
        ✅

Validation
        ✅

Atomicity
        ✅

Wallet Concurrency
        ✅

User Isolation
        ✅

Payout Configuration
        ✅

Payout Methods
        ✅

Payout Options
        ✅

Payout API
        ✅

Payout Tests
        ✅

Withdrawal Creation
        ✅

Withdrawal Deduction
        ✅

Withdrawal Ledger
        ✅

Withdrawal Processing
        ✅

Withdrawal Approval
        ✅

Withdrawal Rejection
        ✅

Withdrawal Reversal
        ✅

Withdrawal Cancellation
        ✅

Withdrawal Ownership
        ✅

Withdrawal State Validation
        ✅

Withdrawal Tests
        ✅ 14/14

FULL REGRESSION
        ✅ 52/52
```

```text
V3-WITHDRAWAL-SYSTEM-COMPLETE
        │
        ▼
52/52 TESTS PASSING
        │
        ▼
NEXT
PHASE 7 — IDEMPOTENCY + WITHDRAWAL CONCURRENCY
```

**The project must continue from `PHASE 7 — IDEMPOTENCY + WITHDRAWAL CONCURRENCY`. Previously completed Wallet Core, Payout Configuration, and basic Withdrawal System work must be treated as verified baselines and should not be rebuilt.**
