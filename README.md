# VELoop Rewards — Wallet & Withdrawal Backend

> **Project Status:** 🚧 Backend Under Development
> **Current Checkpoint:** `BACKEND-CHECKPOINT-01`
> **Next Phase:** `BACKEND-PHASE-02 — Production Hardening & Requirement Completion`
> **Final Backend Target:** `BACKEND-COMPLETE-100%`
> **Frontend:** Not started — intentionally postponed until backend completion

---

# 1. Purpose of This README

This README is the **continuation and development handoff document** for the VELoop Rewards Wallet & Withdrawal Backend.

It is intentionally written so that a developer can continue development from the current state **without inspecting the entire project from the beginning**.

Before making changes, read this document completely.

This document records:

* Current project architecture
* Technology stack
* Implemented modules
* Implemented functionality
* Database structure
* Security decisions
* Wallet accounting rules
* Withdrawal rules
* Idempotency behavior
* Rate-limiting behavior
* Testing status
* Known limitations
* Remaining requirements
* Next implementation phase
* Phase checkpoints
* Final backend completion criteria

---

# 2. Project Objective

VELoop Rewards requires a secure backend system for managing:

* User accounts
* Wallet balances
* VES
* SVES
* Gems
* Tokens
* Spins
* Wallet transactions
* Rewards and deductions
* Payout configurations
* Withdrawals
* Withdrawal lifecycle
* Payout details
* Balance validation
* Eligibility
* Security
* Idempotency
* Rate limiting
* Audit records
* Financial consistency
* Future scalability

The most important architectural rule is:

> **The backend is the single source of truth for all financial and reward-related operations.**

The frontend must never be trusted to calculate or control:

* Wallet balance
* Required VES
* Withdrawal amount
* Reward amount
* Payout amount
* Payout eligibility
* Transaction status
* Withdrawal status
* Payout configuration

---

# 3. Important Development Rule

## DO NOT restart the project from scratch.

The core wallet and withdrawal backend has already been implemented.

The next developer should **continue from the existing implementation** and complete the remaining requirements.

Do not replace the current wallet architecture unless a concrete requirement or verified defect requires it.

---

# 4. Current Technology Stack

| Component             | Technology                         |
| --------------------- | ---------------------------------- |
| Language              | Java 21                            |
| Framework             | Spring Boot 3.5.16                 |
| Security              | Spring Security                    |
| Authentication        | JWT                                |
| JWT Library           | JJWT 0.12.6                        |
| Password Hashing      | BCrypt                             |
| ORM                   | Spring Data JPA / Hibernate        |
| Database              | MySQL                              |
| Migration             | Flyway                             |
| Validation            | Jakarta Bean Validation            |
| API Documentation     | Springdoc OpenAPI                  |
| Monitoring Foundation | Spring Boot Actuator               |
| Build Tool            | Maven                              |
| Testing               | JUnit + Spring Boot Test + MockMvc |

---

# 5. Current Package Architecture

The backend is organized by domain.

```text
src/main/java/com/veloop/rewards/

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

### Domain responsibilities

| Package       | Responsibility                                        |
| ------------- | ----------------------------------------------------- |
| `auth`        | Registration, login and authentication                |
| `security`    | JWT, Spring Security and authorization                |
| `user`        | User entity and user persistence                      |
| `wallet`      | Wallet balances and wallet ledger                     |
| `withdrawal`  | Withdrawal creation and lifecycle                     |
| `payout`      | Payout methods and payout options                     |
| `idempotency` | Duplicate request protection                          |
| `audit`       | Financial/security audit records                      |
| `common`      | Shared exceptions, utilities and common functionality |
| `config`      | Application/security/OpenAPI configuration            |

---

# 6. Current Architecture

The current logical architecture is:

```text
                    CLIENT
                      │
                      ▼
              REST API / Controller
                      │
                      ▼
              Authentication /
               Authorization
                      │
                      ▼
                  Service
                     Layer
                      │
          ┌───────────┼────────────┐
          │           │            │
          ▼           ▼            ▼
       Wallet      Withdrawal    Payout
       Service      Service    Configuration
          │           │            │
          └───────────┼────────────┘
                      │
                      ▼
                 Transaction
                   Boundary
                      │
          ┌───────────┼─────────────┐
          │           │             │
          ▼           ▼             ▼
       MySQL       Ledger        Audit Log
```

---

# 7. Authentication

Authentication is already implemented.

## Registration

```http
POST /api/auth/register
```

Registration currently:

1. Normalizes email
2. Checks duplicate email
3. Hashes password using BCrypt
4. Creates a USER account
5. Creates the user's wallet

---

## Login

```http
POST /api/auth/login
```

Login returns a JWT.

JWT contains authenticated user information including:

* User ID
* Email
* Role
* Issued timestamp
* Expiration

---

## Current User

```http
GET /api/auth/me
```

Returns the currently authenticated user's information.

---

# 8. Security Model

Spring Security is configured as a stateless security system.

Current behavior:

```text
Public
 ├── Register
 ├── Login
 └── API documentation

Authenticated
 └── Protected APIs

ADMIN
 └── Administrative withdrawal operations
```

CSRF/form login/basic authentication are disabled because the backend uses stateless JWT authentication.

Method-level authorization is enabled.

---

# 9. User Model

The User entity currently contains:

```text
id
email
passwordHash
name
role
accountStatus
verified
level
currentRank
createdAt
updatedAt
```

Email is unique.

Important user states include:

```text
ACTIVE
```

and verification status is used by withdrawal eligibility.

---

# 10. Wallet Model

Each user has one wallet.

Wallet currently contains:

```text
id
user
ves
sves
gems
tokens
spins
withdrawnVes
version
```

The wallet uses optimistic locking:

```java
@Version
```

This is important.

### DO NOT remove optimistic locking.

Wallet balances are financial/reward state and must be protected against concurrent modifications.

---

# 11. Supported Currencies

The current currency enum contains:

```text
VES
SVES
GEMS
TOKENS
SPINS
```

All balances are stored and controlled by the backend.

---

# 12. Wallet APIs Already Implemented

## Wallet

```http
GET /api/wallet
```

Returns the authenticated user's wallet.

---

## Wallet Summary

```http
GET /api/wallet/summary
```

Returns wallet summary information.

---

## Transaction History

```http
GET /api/wallet/transactions?page=1&limit=20
```

Transaction history is paginated.

Maximum page size is restricted.

Transactions are returned in descending creation-time order.

---

## Wallet Credit

```http
POST /api/wallet/credit
```

Protected wallet mutation endpoint.

---

## Wallet Debit

```http
POST /api/wallet/debit
```

Protected wallet mutation endpoint.

---

# 13. Wallet Mutation Rules

Wallet operations must follow:

```text
Request
   ↓
Authentication
   ↓
Authorization
   ↓
Input Validation
   ↓
Validate Amount
   ↓
Validate Balance
   ↓
Update Wallet
   ↓
Create Ledger Transaction
   ↓
Create Audit Record
   ↓
Commit
```

Wallet mutation service methods are transactional.

---

# 14. Wallet Ledger

The ledger is a core part of the system.

Every wallet balance change must have a corresponding transaction record.

The `WalletTransaction` model contains:

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

---

# 15. Transaction Types

## Credits

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

## Debits

```text
WITHDRAWAL
EXCHANGE_DEBIT
ADMIN_DEBIT
CORRECTION
```

---

# 16. Transaction Statuses

```text
PENDING
COMPLETED
FAILED
REVERSED
```

---

# 17. Wallet Accounting Rule

The wallet must always remain mathematically consistent.

```text
Current Balance
=
Opening Balance
+ Credits
- Debits
± Corrections
```

The ledger is the historical source used to verify this relationship.

---

# 18. Wallet Concurrency

The current implementation uses:

* Database transactions
* JPA optimistic locking
* `@Version`
* Concurrency tests
* Atomic wallet mutations

The intended behavior is:

```text
Concurrent Request A ──┐
                       ├── Wallet Version Check
Concurrent Request B ──┘
```

Only valid transactions should commit.

### Important

Do not replace this with simple:

```text
read balance
↓
subtract
↓
save
```

without concurrency protection.

That would create a race-condition risk.

---

# 19. Payout Architecture

The payout system is backend controlled.

Current entities:

```text
PayoutMethod
PayoutOption
```

A payout option contains:

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

The client must select a valid backend-configured option.

The client cannot send an arbitrary:

```text
requiredVes
payoutAmount
```

and expect the backend to trust it.

---

# 20. Current Payout Methods

Current configuration contains:

```text
UPI                  ACTIVE
Amazon Gift Card     ACTIVE
Google Play Card     ACTIVE
PayPal               INACTIVE
```

---

# 21. Current UPI Configuration

The currently configured UPI redemption values are:

| Payout Amount | Required VES |
| ------------: | -----------: |
|           ₹10 |        2,400 |
|           ₹25 |        5,800 |
|           ₹50 |       10,000 |
|          ₹100 |       19,500 |
|          ₹150 |       28,500 |
|          ₹300 |       52,500 |
|          ₹500 |       80,500 |
|        ₹1,000 |      150,000 |

These values belong to backend configuration.

---

# 22. Payout Configuration API

Current configuration endpoint:

```http
GET /api/payouts/configuration
```

The backend resolves the selected payout option and determines:

```text
Payout Method
Payout Amount
Currency
Required Currency Amount
Active/Inactive
```

---

# 23. Payout Detail Validation

A dedicated:

```text
PayoutDetailValidator
```

exists.

Current implementation has explicit UPI validation.

Other payout methods require additional method-specific validation during the next phase.

---

# 24. Withdrawal Model

The Withdrawal entity contains:

```text
withdrawalId
user
payoutMethod
payoutOption
currency
currencyAmount
payoutAmount
payoutDetails
status
rejectionReason
reviewNote
transaction
requestedAt
processedAt
createdAt
updatedAt
```

---

# 25. Withdrawal Creation

Current endpoint:

```http
POST /api/withdrawals
```

The client submits:

```text
payoutMethodId
payoutOptionId
payoutDetails
```

The backend then:

1. Authenticates user
2. Checks idempotency
3. Resolves payout method
4. Resolves payout option
5. Resolves required VES from database
6. Validates payout details
7. Checks user eligibility
8. Checks wallet balance
9. Deducts VES
10. Creates ledger transaction
11. Creates withdrawal
12. Creates audit record

---

# 26. Current Withdrawal Strategy

The project currently uses:

> **Immediate Deduction**

Meaning:

```text
Withdrawal Created
       ↓
VES Deducted Immediately
       ↓
Ledger Transaction Created
       ↓
Withdrawal = PENDING
```

The system does not currently use a separate wallet-hold/reservation balance.

This decision should remain consistent throughout the project unless deliberately redesigned.

---

# 27. Withdrawal Statuses

Current statuses:

```text
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
```

---

# 28. Withdrawal Lifecycle

Normal flow:

```text
PENDING
   ↓
PROCESSING
   ↓
APPROVED
```

Cancellation:

```text
PENDING
   ↓
CANCELLED
   ↓
VES Reversal
```

Rejection:

```text
PENDING / PROCESSING
   ↓
REJECTED
   ↓
VES Reversal
```

---

# 29. Withdrawal Reversal

When a withdrawal is rejected or cancelled, the original deducted amount is used to create a correction credit.

The correction transaction uses:

```text
Transaction Type:
CORRECTION
```

and an appropriate withdrawal-related source/reference.

### Important

Never simply modify or delete the original withdrawal ledger transaction.

The correction must remain auditable.

---

# 30. Withdrawal Authorization

Users can access only their own withdrawals.

Administrative lifecycle operations require:

```text
ROLE_ADMIN
```

Current administrative operations include:

```http
PATCH /api/withdrawals/{id}/processing
PATCH /api/withdrawals/{id}/approve
PATCH /api/withdrawals/{id}/reject
```

---

# 31. Withdrawal Eligibility

A dedicated:

```text
WithdrawalEligibilityService
```

currently validates conditions including:

* User exists
* Account is active
* User is verified

Additional eligibility rules should be added in the next phase where required by the final business requirements.

---

# 32. Idempotency

Withdrawal creation requires:

```http
Idempotency-Key
```

Missing key is rejected.

The system stores:

```text
user
idempotencyKey
requestFingerprint
withdrawal
```

The request fingerprint is based on the withdrawal request data.

---

# 33. Idempotency Behavior

### Same user + same key + same request

Return/reuse the existing withdrawal.

### Same user + same key + different request

Reject with an idempotency conflict.

This prevents:

```text
Double click
Network retry
Client retry
Repeated submission
```

from creating multiple withdrawals.

---

# 34. Rate Limiting

Rate limiting is already implemented.

### Algorithm

Current implementation:

> **In-memory fixed-window counter**

The service uses in-memory concurrent state and synchronized checking.

---

## Current Limits

| Operation           |           Limit |
| ------------------- | --------------: |
| Login               |  5 / 60 seconds |
| Withdrawal Creation |  5 / 60 seconds |
| Wallet Mutation     | 20 / 60 seconds |
| Withdrawal Mutation | 20 / 60 seconds |

Rate limiting uses:

* IP-based identification for applicable unauthenticated endpoints
* User-based identification for applicable authenticated sensitive endpoints

When exceeded:

```http
429 TOO MANY REQUESTS
```

A `Retry-After` response value is also provided.

---

# 35. Important Rate Limiting Limitation

The current implementation is **not distributed**.

Example:

```text
              Load Balancer
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
     Backend #1           Backend #2
       Counter               Counter
         A                     B
```

Each server has its own counter.

Therefore, the next phase must introduce distributed rate limiting, most likely using Redis.

Do not consider the current rate limiter equivalent to production distributed rate limiting.

---

# 36. Audit Logging

Audit infrastructure already exists.

Important events include:

```text
WALLET_CREDIT
WALLET_DEBIT
WITHDRAWAL_CREATED
WITHDRAWAL_PROCESSING
WITHDRAWAL_APPROVED
WITHDRAWAL_REJECTED
WITHDRAWAL_CANCELLED
```

Audit data should remain append-oriented and traceable.

---

# 37. Database Architecture

Current migration sequence:

```text
V1  users
V2  wallets
V3  wallet_transactions
V4  payout_configuration
V5  withdrawals
V6  withdrawal_idempotency
V7  withdrawal_audit
V8  audit_log
```

Hibernate schema auto-generation is disabled.

Flyway owns schema evolution.

---

# 38. Important Database Indexes

Indexes currently exist around important lookup fields including:

```text
user_id
wallet_id
transaction_id
reference_id
created_at
status
withdrawal_id
payout_option
audit actor
audit target
idempotency key
```

These indexes should be preserved and reviewed before introducing large-scale features.

---

# 39. Error Handling

The project has centralized REST exception handling.

Important business exceptions include:

```text
AuthenticationFailedException
BusinessException
InsufficientBalanceException
InvalidAmountException
InvalidWithdrawalRequestException
InvalidWithdrawalStateException
IdempotencyConflictException
WithdrawalConcurrencyException
WithdrawalNotFoundException
WithdrawalOwnershipException
WithdrawalTransactionException
```

The next phase should verify that sensitive internal details are not exposed to clients.

---

# 40. Current Test Coverage

The repository already contains tests covering:

### Wallet

```text
WalletServiceIntegrationTest
WalletAtomicityIntegrationTest
WalletConcurrencyIntegrationTest
WalletConsistencyIntegrationTest
WalletIsolationIntegrationTest
WalletJwtIsolationIntegrationTest
WalletCreditApiIntegrationTest
WalletDebitApiIntegrationTest
WalletSummaryApiIntegrationTest
WalletTransactionApiIntegrationTest
WalletTransactionHistoryIntegrationTest
WalletTransactionRepositoryTest
WalletTransactionServiceTest
```

### Withdrawal

```text
WithdrawalServiceIntegrationTest
WithdrawalConcurrencyIntegrationTest
WithdrawalApiIntegrationTest
WithdrawalEligibilityServiceTest
```

### Payout

```text
PayoutConfigurationApiIntegrationTest
PayoutDetailValidatorTest
```

### Rate Limiting

```text
RateLimitFilterTest
RateLimitServiceTest
```

---

# 41. Existing Test Evidence

Existing Surefire test reports in the project show:

```text
Tests:     100
Failures:  0
Errors:    0
Skipped:   0
```

This is existing repository evidence.

Before final completion, the complete suite must be executed again in the actual development environment.

---

# 42. Current Backend Completion State

The current backend is approximately:

> **~90% complete against the full backend requirement scope.**

This is **not** a claim that every production-level requirement is complete.

The core functionality is substantially implemented.

The remaining work is primarily around:

```text
Production Configuration
Payout Completeness
Financial Reconciliation
Fraud / Abuse Protection
Distributed Rate Limiting
Payout Processing Architecture
Monitoring
Documentation
Final Security Audit
Final Requirement Verification
```

---

# 43. Known Gaps

These are the known areas that must be addressed.

## GAP-01 — Environment Configuration

Current development configuration contains development-oriented credentials/fallback values.

Required:

```text
.env.example
Environment-based secrets
No committed production credentials
No fallback production JWT secret
```

---

## GAP-02 — Payout Configuration Completeness

Amazon and Google payout methods exist, but their complete option configuration still needs to be defined.

Method-specific validation also needs to be completed.

---

## GAP-03 — Financial Reconciliation

A dedicated reconciliation process is not yet complete.

Required:

```text
Ledger total
      ↓
Expected balance
      ↓
Compare with wallet balance
      ↓
Detect mismatch
      ↓
Audit/report mismatch
```

---

## GAP-04 — Fraud / Abuse Detection

Basic security exists, but advanced financial abuse detection is not yet complete.

Required future controls include:

* Withdrawal frequency analysis
* Suspicious activity detection
* Repeated failed request detection
* Abnormal wallet activity
* Account-level restrictions
* Administrative review flags

---

## GAP-05 — Distributed Rate Limiting

Current:

```text
In-memory fixed window
```

Target:

```text
Redis / distributed shared state
```

---

## GAP-06 — Payout Processing

Current backend manages the withdrawal lifecycle but does not yet implement a complete asynchronous external payout-processing architecture.

Future:

```text
Withdrawal
    ↓
Queue
    ↓
Payout Worker
    ↓
Provider
    ↓
Result
    ↓
Withdrawal Status
```

---

## GAP-07 — Monitoring

Actuator exists, but complete financial/operational observability still needs to be implemented.

---

## GAP-08 — Documentation

Existing API documentation needs to be synchronized with the current source implementation.

Database and architecture documentation should also be finalized.

---

## GAP-09 — Withdrawal Accounting Review

The `withdrawnVes` wallet field exists.

Its relationship with the actual withdrawal ledger should be reviewed and explicitly defined.

Do not assume it is correct without verifying its update behavior.

---

# 44. Important Point Requiring Verification

The wallet credit/debit request DTO contains a `userId`.

Before changing this behavior, verify whether the controller/service intentionally derives the target user from the authenticated principal or whether administrative credit/debit operations are intended to target another user.

This must be resolved during the final security audit.

Do not blindly change it without understanding the intended authorization model.

---

# 45. NEXT PHASE

# `BACKEND-PHASE-02`

## Production Hardening & Full Requirement Completion

This is the phase to implement next.

The goal is to move from:

```text
CORE BACKEND FUNCTIONAL
```

to:

```text
FULL BACKEND REQUIREMENTS COMPLETE
```

---

# 46. Phase 02 — Implementation Order

Follow this order.

Do not randomly implement features.

```text
02.1 Configuration Hardening
        ↓
02.2 Payout Configuration Completion
        ↓
02.3 Wallet / Withdrawal Accounting Review
        ↓
02.4 Distributed Rate Limiting
        ↓
02.5 Fraud & Abuse Protection
        ↓
02.6 Financial Reconciliation
        ↓
02.7 Payout Processing Architecture
        ↓
02.8 Monitoring & Observability
        ↓
02.9 API Documentation
        ↓
02.10 Architecture / DB Documentation
        ↓
02.11 Full Security Audit
        ↓
02.12 Full Test & Requirement Audit
        ↓
BACKEND-COMPLETE-100%
```

---

# 47. Phase 02.1 — Configuration Hardening

### Implement

* [ ] Create `.env.example`
* [ ] Move DB credentials to environment configuration
* [ ] Move JWT secret to environment configuration
* [ ] Remove fallback JWT secret
* [ ] Remove hard-coded development credentials where inappropriate
* [ ] Review CORS
* [ ] Review Actuator exposure
* [ ] Review production logging
* [ ] Review error responses

### Completion checkpoint

```text
CHECKPOINT-02.1
CONFIGURATION-HARDENED
```

---

# 48. Phase 02.2 — Payout Configuration Completion

### Implement

* [ ] Complete Amazon Gift Card options
* [ ] Complete Google Play Gift Card options
* [ ] Define PayPal behavior
* [ ] Add method-specific validation
* [ ] Validate active/inactive configuration
* [ ] Validate currency
* [ ] Validate required currency amount
* [ ] Add tests for every payout method

### Completion checkpoint

```text
CHECKPOINT-02.2
PAYOUT-CONFIGURATION-COMPLETE
```

---

# 49. Phase 02.3 — Wallet & Withdrawal Accounting Review

### Implement

* [ ] Verify every debit creates a ledger record
* [ ] Verify every credit creates a ledger record
* [ ] Verify rejection reversal
* [ ] Verify cancellation reversal
* [ ] Verify correction transactions
* [ ] Review `withdrawnVes`
* [ ] Verify balance-after values
* [ ] Verify reference IDs
* [ ] Verify transaction status transitions
* [ ] Add missing consistency tests

### Completion checkpoint

```text
CHECKPOINT-02.3
WALLET-ACCOUNTING-COMPLETE
```

---

# 50. Phase 02.4 — Distributed Rate Limiting

Replace or extend current in-memory rate limiting.

### Target

```text
Application Instances
        │
        ▼
      Redis
        │
        ▼
Shared Rate Limit State
```

### Implement

* [ ] Redis integration
* [ ] Distributed counters
* [ ] User-based limits
* [ ] IP-based limits
* [ ] Retry-After support
* [ ] Redis failure strategy
* [ ] Distributed tests

### Completion checkpoint

```text
CHECKPOINT-02.4
DISTRIBUTED-RATE-LIMITING-COMPLETE
```

---

# 51. Phase 02.5 — Fraud & Abuse Protection

### Implement

At minimum evaluate:

```text
Repeated withdrawals
Rapid withdrawals
Repeated failed requests
Suspicious account activity
Unusual wallet activity
Multiple suspicious payout requests
```

Add:

* [ ] Fraud/risk service
* [ ] Risk rules
* [ ] Review flags
* [ ] Audit events
* [ ] Administrative visibility
* [ ] Tests

### Completion checkpoint

```text
CHECKPOINT-02.5
FRAUD-PROTECTION-COMPLETE
```

---

# 52. Phase 02.6 — Financial Reconciliation

Implement a reconciliation service.

### Required logic

```text
Wallet Balance
       │
       ▼
Compare
       ▲
       │
Ledger-Derived Balance
```

If different:

```text
RECONCILIATION FAILURE
```

must be recorded.

### Implement

* [ ] Reconciliation service
* [ ] Expected balance calculation
* [ ] Wallet vs ledger comparison
* [ ] Mismatch detection
* [ ] Audit record
* [ ] Administrative report/endpoint if appropriate
* [ ] Automated tests

### Completion checkpoint

```text
CHECKPOINT-02.6
FINANCIAL-RECONCILIATION-COMPLETE
```

---

# 53. Phase 02.7 — Payout Processing

Introduce asynchronous processing architecture.

### Target

```text
POST /withdrawals
        ↓
Transaction
        ↓
PENDING
        ↓
Queue
        ↓
Payout Worker
        ↓
External Provider
        ↓
Result
        ↓
APPROVED / REJECTED
```

### Implement

* [ ] Queue abstraction
* [ ] Payout job
* [ ] Worker
* [ ] Retry
* [ ] Failure handling
* [ ] Idempotent processing
* [ ] Provider abstraction
* [ ] Audit events
* [ ] Tests

### Important

Do not remove the existing withdrawal transaction safety while introducing asynchronous processing.

### Completion checkpoint

```text
CHECKPOINT-02.7
PAYOUT-PROCESSING-COMPLETE
```

---

# 54. Phase 02.8 — Monitoring & Observability

### Implement

* [ ] Health checks
* [ ] Application metrics
* [ ] Wallet metrics
* [ ] Withdrawal metrics
* [ ] Rate-limit metrics
* [ ] Error metrics
* [ ] Reconciliation metrics
* [ ] Structured logs
* [ ] Request/correlation IDs

Useful metrics:

```text
wallet.credit.success
wallet.credit.failure
wallet.debit.success
wallet.debit.failure

withdrawal.created
withdrawal.processing
withdrawal.approved
withdrawal.rejected
withdrawal.cancelled

rate_limit.blocked

reconciliation.success
reconciliation.failure
```

### Completion checkpoint

```text
CHECKPOINT-02.8
OBSERVABILITY-COMPLETE
```

---

# 55. Phase 02.9 — API Documentation

Update:

```text
API_DOCUMENTATION.md
```

It must match the actual source code.

Document:

* Endpoint
* HTTP method
* Authentication
* Authorization
* Request
* Response
* Validation
* Errors
* Pagination
* Idempotency
* Rate limits
* Withdrawal lifecycle

### Completion checkpoint

```text
CHECKPOINT-02.9
API-DOCUMENTATION-COMPLETE
```

---

# 56. Phase 02.10 — Architecture Documentation

Create/finalize:

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

These documents should describe the actual implementation.

### Completion checkpoint

```text
CHECKPOINT-02.10
ARCHITECTURE-DOCUMENTATION-COMPLETE
```

---

# 57. Phase 02.11 — Final Security Audit

Check every sensitive operation.

### Authentication

* [ ] JWT validation
* [ ] Expiration
* [ ] Invalid token handling
* [ ] Password hashing

### Authorization

* [ ] USER access
* [ ] ADMIN access
* [ ] Ownership
* [ ] Cross-user access prevention

### Wallet

* [ ] No client-controlled balance
* [ ] No negative balance
* [ ] Atomic operations
* [ ] Ledger consistency
* [ ] Concurrency safety

### Withdrawal

* [ ] Client cannot choose arbitrary VES
* [ ] Client cannot manipulate payout amount
* [ ] Client cannot access another user's withdrawal
* [ ] Duplicate request protection
* [ ] Rate limiting
* [ ] Eligibility
* [ ] Reversal
* [ ] Audit

### Configuration

* [ ] No secrets committed
* [ ] Environment variables
* [ ] Secure JWT configuration
* [ ] Secure DB configuration

### Completion checkpoint

```text
CHECKPOINT-02.11
SECURITY-AUDIT-COMPLETE
```

---

# 58. Phase 02.12 — Final Requirement Audit

Compare the implementation against the original VELoop requirements one by one.

### Wallet

* [ ] Wallet exists for every user
* [ ] All currencies supported
* [ ] Backend controls balances
* [ ] Credit implemented
* [ ] Debit implemented
* [ ] Ledger implemented
* [ ] Balance validation implemented
* [ ] Transaction history implemented
* [ ] Summary implemented

### Payout

* [ ] Payout methods
* [ ] Payout options
* [ ] Backend-controlled amounts
* [ ] Active/inactive state
* [ ] Method-specific validation
* [ ] Eligibility

### Withdrawal

* [ ] Withdrawal creation
* [ ] Balance check
* [ ] Immediate deduction
* [ ] Pending state
* [ ] Processing
* [ ] Approval
* [ ] Rejection
* [ ] Cancellation
* [ ] Reversal
* [ ] Audit
* [ ] Idempotency

### Security

* [ ] JWT
* [ ] Authorization
* [ ] User isolation
* [ ] Validation
* [ ] Rate limiting
* [ ] Duplicate protection
* [ ] Audit

### Reliability

* [ ] Atomic transactions
* [ ] Concurrency protection
* [ ] Reconciliation
* [ ] Error handling
* [ ] Monitoring

### Documentation

* [ ] API documentation
* [ ] DB documentation
* [ ] Architecture documentation
* [ ] Environment example

### Testing

* [ ] Normal wallet credit
* [ ] Normal wallet debit
* [ ] Insufficient balance
* [ ] Duplicate withdrawal
* [ ] Concurrent withdrawal
* [ ] Invalid payout
* [ ] User isolation
* [ ] Reversal
* [ ] API manipulation
* [ ] Rate limiting
* [ ] Reconciliation

---

# 59. FINAL BACKEND CHECKPOINT

The backend must not be marked complete merely because the APIs work.

The final checkpoint requires:

```text
All Requirements
       +
Implementation
       +
Security
       +
Accounting
       +
Concurrency
       +
Idempotency
       +
Fraud Protection
       +
Reconciliation
       +
Rate Limiting
       +
Monitoring
       +
Documentation
       +
Testing
       +
Final Audit
       │
       ▼
BACKEND-COMPLETE-100%
```

---

# 60. What Happens After `BACKEND-COMPLETE-100%`

Only after the final backend checkpoint:

```text
BACKEND-COMPLETE-100%
        ↓
Frontend Phase Begins
        ↓
Wallet UI
        ↓
Transaction History
        ↓
Withdrawal UI
        ↓
Payout Selection
        ↓
Payout Details
        ↓
Withdrawal Status
```

The frontend will consume the existing backend APIs.

The frontend must not introduce financial business logic that conflicts with backend rules.

---

# 61. Development Rules for Future Contributors

## Rule 1 — Do not trust the frontend

All financial values must be calculated/validated by the backend.

---

## Rule 2 — Do not directly modify wallet balances

Wallet changes must go through the wallet service.

---

## Rule 3 — Every wallet change requires a ledger record

No silent balance modification.

---

## Rule 4 — Financial operations must be transactional

Do not split wallet and ledger updates into unrelated database operations.

---

## Rule 5 — Preserve idempotency

Do not remove `Idempotency-Key` protection from withdrawal creation.

---

## Rule 6 — Preserve concurrency protection

Do not remove optimistic locking without replacing it with an equivalent or stronger strategy.

---

## Rule 7 — Never delete financial history to fix a mistake

Use correction/reversal transactions.

---

## Rule 8 — Preserve user isolation

Never allow a normal USER request to access another user's wallet or withdrawal.

---

## Rule 9 — Backend owns payout configuration

Do not accept required VES or payout amounts directly from the client as trusted values.

---

## Rule 10 — Update tests with every financial change

Any modification to wallet or withdrawal behavior must have corresponding tests.

---

# 62. Recommended Development Workflow

For every next-phase task:

```text
1. Read this README
        ↓
2. Identify the relevant existing module
        ↓
3. Inspect only the related classes/tests
        ↓
4. Implement the change
        ↓
5. Add/update tests
        ↓
6. Run complete test suite
        ↓
7. Verify database migration if required
        ↓
8. Update documentation
        ↓
9. Mark phase checkpoint
        ↓
10. Continue to next phase
```

Do not unnecessarily rewrite unrelated modules.

---

# 63. Current Starting Point for the Next Developer

The next developer should begin here:

```text
CURRENT CHECKPOINT
==================
BACKEND-CHECKPOINT-01


START NEXT
==========
BACKEND-PHASE-02


FIRST TASK
==========
Phase 02.1
Configuration & Environment Hardening


THEN
====
02.2 Payout Configuration
02.3 Accounting Review
02.4 Distributed Rate Limiting
02.5 Fraud Protection
02.6 Reconciliation
02.7 Payout Processing
02.8 Monitoring
02.9 API Documentation
02.10 Architecture Documentation
02.11 Security Audit
02.12 Final Requirement Audit


FINAL TARGET
============
BACKEND-COMPLETE-100%


AFTER FINAL TARGET
==================
START FRONTEND
```

---

# 64. Definition of Done

A phase is **not complete** until all of the following are true:

```text
[ ] Code implemented
[ ] Existing architecture preserved
[ ] Required tests added
[ ] Existing tests still pass
[ ] Database changes migrated
[ ] Security reviewed
[ ] Error handling reviewed
[ ] Documentation updated
[ ] Requirement verified
[ ] Checkpoint marked
```

---

# 65. Project State Summary

```text
PROJECT
=======
VELoop Rewards Wallet & Withdrawal Backend


STACK
=====
Java 21
Spring Boot 3.5.16
Spring Security
JWT
JPA / Hibernate
MySQL
Flyway
Maven


CORE STATUS
===========
Authentication              COMPLETE
Authorization              COMPLETE
User Management             COMPLETE
Wallet                      COMPLETE
Multi-Currency              COMPLETE
Wallet Ledger               COMPLETE
Credit / Debit              COMPLETE
Balance Validation          COMPLETE
Concurrency Protection      COMPLETE
Payout Foundation           COMPLETE
Withdrawal System           COMPLETE
Withdrawal Lifecycle        COMPLETE
Idempotency                 COMPLETE
User Isolation              COMPLETE
Rate Limiting               IMPLEMENTED
Audit Logging               IMPLEMENTED
Testing                     IMPLEMENTED


REMAINING
=========
Configuration Hardening
Payout Completion
Accounting Review
Distributed Rate Limiting
Fraud Protection
Financial Reconciliation
Payout Processing
Monitoring
Documentation Completion
Final Security Audit
Final Requirement Audit


CURRENT CHECKPOINT
==================
BACKEND-CHECKPOINT-01


NEXT CHECKPOINT
===============
BACKEND-PHASE-02


FINAL BACKEND CHECKPOINT
========================
BACKEND-COMPLETE-100%


FRONTEND
========
NOT STARTED
INTENTIONALLY BLOCKED UNTIL BACKEND COMPLETION
```

---

# 66. Final Instruction

**Do not start frontend development yet.**

The immediate objective is:

> **Complete `BACKEND-PHASE-02` and reach `BACKEND-COMPLETE-100%` against the original VELoop backend requirements.**

Once the final backend checkpoint is achieved, the project can safely move to frontend development.

**This README should be updated after every major phase so that the next developer can continue from the latest checkpoint without re-auditing the entire project.**
