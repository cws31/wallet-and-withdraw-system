# VELoop Rewards — Wallet & Withdrawal Backend

> **Development Status:** IN PROGRESS
>
> A secure, backend-driven Wallet & Withdrawal system for VELoop Rewards, built incrementally with phase-based development checkpoints.
>
> **Current Checkpoint:** `V2-PAYOUT-CONFIGURATION-COMPLETE`
>
> **Current Phase:** Payout Configuration completed and verified
>
> **Next Phase:** Withdrawal System

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

The project is being developed as an **independent development/demo environment**. It does not connect to VELoop production systems or production databases.

The backend is always the **single source of truth**.

The frontend must never be able to directly modify a wallet balance.

For example, changing:

```text
VEs: 1000 → 100000
```

in browser storage or frontend JavaScript must never change the actual wallet balance stored by the backend.

---

# 2. Project Objective

The final system should provide this high-level flow:

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
 ├── Idempotency
 ├── Wallet deduction
 └── Ledger transaction
 │
 ▼
PENDING
 │
 ▼
PROCESSING
 │
 ├── APPROVED
 └── REJECTED
```

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

The frontend must never be trusted for financial values.

For example, the frontend may send:

```json
{
  "method": "UPI",
  "optionId": 1
}
```

The backend must look up the actual payout option and determine:

```text
Payout value
Required VES
Currency
Availability
Eligibility
```

The frontend must not be able to simply send:

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

---

# 5. Current Checkpoint

## `V2-PAYOUT-CONFIGURATION-COMPLETE`

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
CURRENT CHECKPOINT
```

### Checkpoint verification

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
| Full regression suite           | ✅ **38/38 passing** |

---

# 6. Technology Stack

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

# 7. Current Architecture

```text
                         React + Vite
                              │
                              │ REST + JWT
                              ▼
                    ┌─────────────────────┐
                    │   Spring Boot API   │
                    └──────────┬──────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
                ▼              ▼              ▼
             Auth           Wallet          Payout
                │              │              │
                ▼              ▼              ▼
             User          Ledger       Configuration
                               │              │
                               │              │
                               └──────┬───────┘
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

# 8. Current Package Architecture

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

# 9. Wallet Core

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

# 10. Wallet Database Design

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

# 11. Wallet Ledger

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

Example:

```text
Before: 10,000 VES
Credit:  500 VES
After:  10,500 VES
```

Ledger:

```text
Currency:       VES
Type:           REWARD
Amount:         500
Balance Before: 10,000
Balance After:  10,500
Source:         REWARD
Status:         COMPLETED
```

---

# 12. Transaction Types

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

# 13. Wallet Operations

## Credit

```text
Request
   ↓
Validate request
   ↓
Load wallet
   ↓
Read balance
   ↓
Calculate new balance
   ↓
Update wallet
   ↓
Create ledger
   ↓
Commit
```

## Debit

```text
Request
   ↓
Validate request
   ↓
Load wallet
   ↓
Validate balance
   ↓
Calculate new balance
   ↓
Update wallet
   ↓
Create ledger
   ↓
Commit
```

Wallet and ledger changes occur inside the same transaction.

If a critical operation fails, the transaction is rolled back.

---

# 14. Wallet Security

## JWT Identity

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

## User Isolation

User A cannot access User B's wallet.

This has been explicitly tested at both:

```text
Service level
API/JWT level
```

## Authorization

Wallet mutations:

```text
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

# 15. Balance Validation

Every debit validates the balance on the backend.

Example:

```text
Available: 1,900 VES
Requested: 2,400 VES
```

Result:

```text
InsufficientBalanceException
```

The wallet remains unchanged.

No successful ledger transaction is created.

Negative balances are not allowed.

---

# 16. Atomicity & Concurrency

Wallet update and ledger creation are transactional.

Atomicity test:

```text
Wallet update
     +
Ledger creation
```

If ledger creation fails:

```text
ROLLBACK
```

Concurrency test:

```text
Initial balance = 10,000 VES

Request A = 8,000 VES
Request B = 8,000 VES
```

Both cannot successfully deduct the same balance.

Optimistic locking prevents the lost-update scenario.

---

# 17. Wallet APIs

## Get Wallet

```http
GET /api/wallet
```

Authentication:

```text
Bearer JWT
```

Returns the authenticated user's wallet.

---

## Get Transactions

```http
GET /api/wallet/transactions?page=1&limit=20
```

Supports:

* Pagination
* Newest-first transaction ordering
* User isolation

---

## Get Summary

```http
GET /api/wallet/summary
```

Returns:

```text
VES
SVES
Gems
Tokens
Spins
Total Transactions
```

---

## Credit Wallet

```http
POST /api/wallet/credit
```

Protected:

```text
ADMIN
```

---

## Debit Wallet

```http
POST /api/wallet/debit
```

Protected:

```text
ADMIN
```

---

# 18. Transaction Pagination

Wallet transactions use a stable backend response:

```json
{
  "content": [],
  "page": 1,
  "limit": 20,
  "totalElements": 0,
  "totalPages": 0,
  "hasNext": false,
  "hasPrevious": false
}
```

The API limits the requested page size to prevent unnecessarily large responses.

---

# 19. Wallet Summary

The wallet summary contains:

```text
VES
SVES
Gems
Tokens
Spins
totalTransactions
```

The summary is generated from backend wallet data and transaction records.

---

# 20. Wallet Test Coverage

The Wallet Core has been tested through integration and API tests.

## Wallet Service

```text
WalletServiceIntegrationTest
8/8 PASS
```

Covers:

* Credit
* Debit
* Insufficient balance
* Zero amount
* Negative amount
* Currency independence
* Credit ledger
* Debit ledger

## Atomicity

```text
WalletAtomicityIntegrationTest
1/1 PASS
```

## Concurrency

```text
WalletConcurrencyIntegrationTest
1/1 PASS
```

## Transaction History

```text
WalletTransactionHistoryIntegrationTest
3/3 PASS
```

## Transaction API

```text
WalletTransactionApiIntegrationTest
3/3 PASS
```

## Wallet Summary API

```text
WalletSummaryApiIntegrationTest
2/2 PASS
```

## Credit API

```text
WalletCreditApiIntegrationTest
4/4 PASS
```

## Debit API

```text
WalletDebitApiIntegrationTest
5/5 PASS
```

## User Isolation

```text
WalletIsolationIntegrationTest
1/1 PASS
```

## JWT Isolation

```text
WalletJwtIsolationIntegrationTest
1/1 PASS
```

## Consistency

```text
WalletConsistencyIntegrationTest
1/1 PASS
```

---

# 21. Payout Configuration

Payout configuration is now implemented as a backend-controlled configuration layer.

The database contains:

```text
payout_methods
        │
        │ 1 : N
        ▼
payout_options
```

This allows payout methods and denominations to be managed independently of frontend code.

---

# 22. Payout Database Design

Migration:

```text
V4__create_payout_configuration.sql
```

Tables:

```text
payout_methods
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

The payout method code is unique.

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

The `method` field references `payout_methods`.

---

# 23. Current Payout Configuration

The current development database contains:

```text
UPI                    ACTIVE
AMAZON_GIFT_CARD       ACTIVE
GOOGLE_PLAY_GIFT_CARD  ACTIVE
PAYPAL                 INACTIVE
```

The backend API exposes only active methods.

Currently configured UPI options:

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

These values are stored in the backend database and are not hardcoded into the frontend.

---

# 24. Payout Configuration API

## Get Payout Configuration

```http
GET /api/payouts/configuration
```

Authentication:

```text
Bearer JWT
```

Returns:

* Active payout methods
* Active payout options
* Payout amount
* Currency
* Required currency amount

Conceptual response:

```json
{
  "success": true,
  "message": "Payout configuration retrieved successfully",
  "data": [
    {
      "id": 1,
      "code": "UPI",
      "name": "UPI",
      "options": [
        {
          "id": 1,
          "payoutAmount": 10,
          "currency": "INR",
          "currencyAmount": 2400
        }
      ]
    }
  ]
}
```

Inactive payout methods are excluded from the response.

---

# 25. Payout Configuration Security

The payout configuration endpoint requires authentication.

```text
Valid JWT
    ↓
200 OK
```

Without JWT:

```text
401 Unauthorized
```

The endpoint is read-only.

The client cannot modify payout configuration through this API.

---

# 26. Payout Configuration Test Coverage

Integration tests are implemented in:

```text
PayoutConfigurationApiIntegrationTest
```

Current coverage:

```text
5/5 PASS
```

Verified:

* Authenticated access
* Active payout methods
* UPI configuration
* All 8 UPI options
* Correct payout amounts
* Correct required VES values
* Ascending payout option order
* Inactive PayPal exclusion
* Unauthorized request rejection

---

# 27. Current Full Test Status

The complete regression suite currently passes:

```text
Tests run: 38
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Breakdown:

```text
Wallet Core tests
33/33 PASS

Payout Configuration tests
5/5 PASS

-------------------------
Total
38/38 PASS
```

This is the current verified baseline for further development.

---

# 28. Phase 6 — Withdrawal System

The next development phase is the Withdrawal module.

The expected withdrawal model will contain:

```text
Withdrawal
├── withdrawalId
├── userId
├── method
├── optionId
├── currency
├── currencyAmount
├── payoutAmount
├── payoutDetails
├── status
├── rejectionReason
├── reviewNote
├── transactionId
├── requestedAt
├── processedAt
└── updatedAt
```

The implementation will validate all payout configuration server-side.

The frontend will send an option identifier, while the backend will resolve the actual payout values from the database.

---

# 29. Withdrawal Status

Required statuses:

```text
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
```

Expected flow:

```text
PENDING
   ↓
PROCESSING
   ↓
APPROVED
```

or:

```text
PENDING
   ↓
REJECTED
```

---

# 30. Withdrawal Wallet Deduction Strategy

The planned strategy is **immediate deduction at withdrawal creation**.

Example:

```text
Available balance: 10,000 VES

Withdrawal:
2,400 VES

Remaining balance:
7,600 VES
```

The withdrawal creation and wallet deduction must be part of the same logical operation.

If the withdrawal is later rejected:

```text
Withdrawal REJECTED
        ↓
Reverse wallet deduction
        ↓
Create reversal ledger entry
```

This strategy will be implemented and tested in the Withdrawal phase.

---

# 31. Idempotency

The withdrawal system must prevent duplicate requests such as:

```text
User clicks Redeem
        ↓
Request 1

User clicks Redeem again
        ↓
Request 2
```

from creating:

```text
2 withdrawals
2 wallet deductions
```

The future implementation will use an idempotency mechanism to guarantee safe retry behavior.

---

# 32. Withdrawal Concurrency

The withdrawal system must also handle concurrent requests.

Example:

```text
Available = 10,000 VES

Request A → 8,000 VES
Request B → 8,000 VES
```

Only one request should be able to consume the available balance.

The existing wallet optimistic-locking mechanism will be integrated into the withdrawal flow.

---

# 33. Audit Logging

Sensitive operations will eventually be auditable.

Important events include:

```text
Withdrawal Created
Withdrawal Approved
Withdrawal Rejected
Wallet Credit
Wallet Debit
Balance Correction
Payout Configuration Changed
```

Planned audit fields include:

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

Audit implementation belongs to the security-hardening phase.

---

# 34. Rate Limiting

Sensitive APIs should eventually receive rate limiting protection, particularly:

* Authentication
* Wallet mutations
* Withdrawal
* OTP/authentication-related endpoints

Rate limiting is planned for the security-hardening phase.

---

# 35. Fraud & Security Checks

Future withdrawal processing must support reasonable security controls including:

* Account status checks
* Eligibility checks
* Duplicate request prevention
* Suspicious activity handling
* Server-side validation
* Payout validation
* Withdrawal validation
* Audit logging

These controls will be introduced incrementally.

---

# 36. Planned Frontend

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

# 37. API Documentation

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
```

Planned withdrawal APIs:

```text
POST /api/withdrawals
GET  /api/withdrawals
GET  /api/withdrawals/{id}
```

Swagger/OpenAPI is already configured for the existing APIs.

---

# 38. Database Migrations

Current migrations:

```text
src/main/resources/db/migration/
│
├── V1__create_users.sql
├── V2__create_wallets.sql
├── V3__create_wallet_transactions.sql
└── V4__create_payout_configuration.sql
```

Future migrations will add:

```text
Withdrawal
Idempotency
Audit
```

as those modules are implemented.

---

# 39. Environment & Security

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

# 40. Production Access Restriction

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

# 41. Scalability Requirement

The final README must answer:

> If VELoop Rewards grows from 1,000 users to 1,000,000 users, what changes would be required?

The final architecture discussion should cover:

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

This section will be expanded after the core system is complete.

---

# 42. Final Testing Requirements

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

Currently verified:

```text
1. Normal credit                 ✅
3. Insufficient balance         ✅
5. Concurrent wallet updates    ✅
7. Another user's wallet        ✅
9. Wallet API manipulation      ✅
```

Payout configuration testing:

```text
Active methods                  ✅
Inactive methods excluded       ✅
Payout option validation data   ✅
Required VES configuration      ✅
Authenticated API access        ✅
```

Withdrawal-specific scenarios will be implemented during Phases 6 and 7.

---

# 43. Development Roadmap

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

# 44. Where to Start When Continuing Development

**Do not restart from project setup.**

The current project is at:

```text
V2-PAYOUT-CONFIGURATION-COMPLETE
```

Before continuing development, verify:

```cmd
mvnw clean test
```

Expected:

```text
Tests run: 38
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

The existing Wallet Core and Payout Configuration implementations should be treated as frozen baselines.

### Next implementation

```text
PHASE 6 — WITHDRAWAL SYSTEM
```

Start with:

```text
Withdrawal database design
        ↓
Withdrawal migration
        ↓
Withdrawal entity
        ↓
Withdrawal enums
        ↓
Withdrawal repository
        ↓
Withdrawal DTOs
        ↓
Withdrawal service
        ↓
Wallet deduction integration
        ↓
Withdrawal API
        ↓
Withdrawal tests
```

Do not implement idempotency or advanced withdrawal concurrency infrastructure before the basic Withdrawal flow is established. Those concerns belong to Phase 7.

---

# 45. Expected Final Repository Structure

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

# 46. Final Submission Checklist

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
* [ ] Withdrawal requests stored
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

## Frontend

* [ ] `/wallet`
* [ ] `/payout`
* [ ] Backend-connected wallet
* [ ] Backend-connected payout options
* [ ] Loading states
* [ ] Error states
* [ ] Wallet refresh after withdrawal

## Documentation

* [x] README development status
* [x] Architecture documentation
* [x] Current API documentation through OpenAPI/Swagger
* [ ] API_DOCUMENTATION.md complete
* [ ] Database/model documentation
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

# 47. Current Checkpoint

```text
V2-PAYOUT-CONFIGURATION-COMPLETE
```

### Verified state

```text
Authentication
        ✅
JWT
        ✅
Authorization
        ✅
OpenAPI / Swagger
        ✅
        │
        ▼
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
Concurrency
        ✅
User Isolation
        ✅
API Security
        ✅
        │
        ▼
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
        │
        ▼
38/38 TESTS PASSING
        │
        ▼
NEXT
Withdrawal System
```

**The project should continue from `PHASE 6 — WITHDRAWAL SYSTEM`. Previously completed Wallet Core and Payout Configuration work should not be rebuilt.**
