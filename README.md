# VELoop Rewards — Wallet & Withdrawal Backend

> **Development Status:** IN PROGRESS
> **Current Checkpoint:** `BACKEND-CHECKPOINT-03 — SECURITY HARDENING VERIFIED`
> **Current Development Position:** Core Wallet + Payout + Withdrawal + Idempotency + Concurrency + Audit Logging + Security Hardening completed and verified.
> **Current Next Phase:** `PHASE-10 — DEMONSTRATION FRONTEND`
> **Backend Strategy:** Backend is the source of truth for all wallet balances, payout configuration, withdrawal amounts and financial state.

---

# 1. Project Overview

VELoop Rewards uses multiple internal reward currencies and redemption mechanisms.

The objective of this project is to develop a complete, secure and scalable wallet and withdrawal system where all financial/reward state is controlled by the backend.

The system manages:

* User wallet balances
* VEs
* SVEs
* Gems
* Tokens
* Spins
* Wallet transactions
* Earnings
* Deductions
* Exchange-related balance changes
* Withdrawal requests
* Withdrawal status
* Payout methods
* Payout options
* User payout details
* Transaction history
* Balance validation
* Withdrawal eligibility
* Security checks
* Audit records

The most important architectural rule is:

> **The backend is always the source of truth.**

The frontend must never be trusted for wallet balances, withdrawal amounts, payout values or eligibility.

---

# 2. Core Security Principle

A user must never be able to modify a wallet value from the browser and receive the modified balance.

For example:

```text
Actual backend balance:

VEs = 1,000
```

A user may attempt:

```text
VEs = 100,000
```

through:

* Browser DevTools
* LocalStorage
* React state
* Modified API request
* Modified JSON payload
* Direct frontend manipulation

The backend must continue using the actual server-side balance.

The same rule applies to:

```text
SVEs
Gems
Tokens
Spins
Withdrawal amount
Payout value
Payout option
Eligibility
Transaction status
```

---

# 3. Project Objective

The project must provide a production-like wallet architecture that supports:

```text
Authentication
      ↓
User
      ↓
Wallet
      ↓
Wallet Ledger
      ↓
Payout Configuration
      ↓
Withdrawal
      ↓
Audit Log
```

The architecture must provide:

* Correct wallet balances
* Server-side validation
* Atomic wallet operations
* Ledger records
* Withdrawal lifecycle
* Idempotency
* Concurrency protection
* Authentication
* Authorization
* Rate limiting
* Input validation
* Withdrawal eligibility checks
* Audit logging
* Ownership protection

---

# 4. Important Development Rule

This repository is being developed **file-by-file**.

Do not rebuild completed modules unnecessarily.

Before changing an existing module:

1. Read the current implementation.
2. Understand its dependencies.
3. Check existing tests.
4. Make the smallest required change.
5. Run the relevant tests.
6. Run the complete regression suite when the phase is complete.
7. Update this README checkpoint.

The objective is not only to complete the project but also to maintain a clean GitHub history and understand every implementation step.

---

# 5. Current Development Status

## Completed

### Phase 1 — Project Setup

Status:

```text
COMPLETED
```

---

### Phase 2 — Database & Flyway

Status:

```text
COMPLETED
```

Implemented:

* Database configuration
* Entity structure
* Repository structure
* Flyway migrations
* Database constraints/indexes where required

---

### Phase 3 — Authentication & JWT

Status:

```text
COMPLETED
```

Implemented:

* User authentication
* JWT authentication
* JWT validation
* Protected APIs
* Authenticated user identification

---

### Phase 3.5 — OpenAPI / Swagger

Status:

```text
COMPLETED
```

Swagger/OpenAPI is available for API verification and development testing.

---

### Phase 4 — Wallet

Status:

```text
COMPLETED
```

Implemented:

* Wallet entity
* Wallet balance management
* Wallet service
* Wallet APIs
* Server-side balance validation
* Wallet transactions
* Wallet ledger
* Credit operations
* Debit operations

---

### Phase 5 — Payout Configuration

Status:

```text
COMPLETED
```

Implemented:

* Payout methods
* Payout options
* Active/inactive configuration
* Backend-controlled payout values
* Payout validation

Current UPI configuration includes:

| Payout | Required VEs |
| ------ | -----------: |
| ₹10    |        2,400 |
| ₹25    |        5,800 |
| ₹50    |       10,000 |
| ₹100   |       19,500 |
| ₹150   |       28,500 |
| ₹300   |       52,500 |
| ₹500   |       80,500 |
| ₹1,000 |      150,000 |

These values are backend/database configuration and must not be trusted from the frontend.

---

### Phase 6 — Withdrawal

Status:

```text
COMPLETED
```

Implemented:

* Withdrawal entity
* Withdrawal creation
* Withdrawal status
* Payout method validation
* Payout option validation
* Balance validation
* Wallet deduction
* Rejection handling
* Cancellation handling
* Withdrawal history

Withdrawal states:

```text
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
```

---

### Phase 7 — Idempotency & Concurrency

Status:

```text
COMPLETED
```

Implemented:

* Idempotency key validation
* Idempotency key normalization
* Request fingerprinting
* Duplicate request protection
* Same-key/same-request handling
* Same-key/different-request conflict handling
* Concurrent withdrawal protection
* Safe wallet deduction

Important behavior:

```text
Same user
+
Same Idempotency-Key
+
Same request
        ↓
Existing withdrawal returned
```

But:

```text
Same user
+
Same Idempotency-Key
+
Different request
        ↓
409 Conflict
```

---

### Phase 8 — Audit Logging

Status:

```text
COMPLETED AND VERIFIED
```

Audit logging has already been manually verified.

No further audit implementation is currently required.

Important audit events include:

```text
WITHDRAWAL_CREATED
WITHDRAWAL_PROCESSING
WITHDRAWAL_APPROVED
WITHDRAWAL_REJECTED
WITHDRAWAL_CANCELLED
```

The audit architecture contains:

* Actor
* Target user
* Target type
* Action
* Reference ID
* Metadata
* Created timestamp

Audit history can be queried by:

* Target user
* Actor
* Target type + reference
* Action

---

# 6. Phase 9 — Security Hardening

Status:

```text
COMPLETED AND VERIFIED
```

Checkpoint:

```text
BACKEND-CHECKPOINT-03 — SECURITY HARDENING VERIFIED
```

Phase 9 completed the remaining core backend security requirements.

---

## 6.1 Rate Limiting

Status:

```text
COMPLETED
```

Implemented:

```text
RateLimitProperties
RateLimitDecision
RateLimitService
RateLimitFilter
```

Tests:

```text
RateLimitServiceTest
8/8 PASS

RateLimitFilterTest
6/6 PASS
```

Current configuration:

```properties
security.rate-limit.enabled=true

security.rate-limit.login-requests=5
security.rate-limit.login-window-seconds=60

security.rate-limit.withdrawal-requests=5
security.rate-limit.withdrawal-window-seconds=60

security.rate-limit.wallet-mutation-requests=20
security.rate-limit.wallet-mutation-window-seconds=60

security.rate-limit.withdrawal-mutation-requests=20
security.rate-limit.withdrawal-mutation-window-seconds=60

security.rate-limit-max-entries=10000
```

The rate-limit filter is integrated into the security chain after JWT authentication.

---

## 6.2 Method-Specific Payout Detail Validation

Status:

```text
COMPLETED
```

Implemented:

```text
WithdrawalCreateRequest
PayoutDetailValidator
```

The request validates:

* Payout method ID
* Payout option ID
* Payout details
* Maximum payout-detail length

UPI validation is implemented.

Example:

```text
user@example
```

is rejected if invalid.

Valid UPI-style input is accepted after trimming whitespace.

Test result:

```text
PayoutDetailValidatorTest
10/10 PASS
```

---

## 6.3 Withdrawal Eligibility

Status:

```text
COMPLETED
```

Implemented:

```text
WithdrawalEligibilityService
```

Current eligibility checks:

```text
User exists
        ↓
Account status = ACTIVE
        ↓
User verified = true
        ↓
Withdrawal allowed
```

Rejected conditions include:

* Null user
* Inactive user
* Suspended user
* Unverified user

Test result:

```text
WithdrawalEligibilityServiceTest
6/6 PASS
```

Important:

> This is basic withdrawal eligibility protection, not a complete fraud-detection engine.

Advanced fraud/risk detection remains future work.

---

## 6.4 Duplicate Withdrawal Protection

Status:

```text
COMPLETED
```

The withdrawal service already supports idempotency.

Flow:

```text
Request
   ↓
Validate Idempotency-Key
   ↓
Normalize Key
   ↓
Create Request Fingerprint
   ↓
Search Existing Request
   ↓
Same Request?
 ┌───────────────┐
 │               │
Yes             No
 │               │
 ↓               ↓
Return          409 Conflict
Existing
Withdrawal
```

---

## 6.5 Security Integration Testing

Status:

```text
COMPLETED
```

Final HTTP-level security integration test:

```text
WithdrawalApiIntegrationTest
14/14 PASS
```

Verified scenarios:

1. Valid HTTP withdrawal
2. Invalid UPI
3. Script-like payout details
4. Blank payout details
5. Oversized payout details
6. Missing JWT
7. Missing Idempotency-Key
8. Malformed JWT
9. Insufficient balance
10. Inactive payout method
11. Inactive payout option
12. Duplicate identical withdrawal
13. Same idempotency key with different request
14. Rate-limit enforcement

---

# 7. Phase 9 Verification Baseline

The following Phase 9-specific test suites are verified:

| Test Suite                             |         Result |
| -------------------------------------- | -------------: |
| `WithdrawalApiIntegrationTest`         | **14/14 PASS** |
| `PayoutDetailValidatorTest`            | **10/10 PASS** |
| `WithdrawalEligibilityServiceTest`     |   **6/6 PASS** |
| `RateLimitServiceTest`                 |   **8/8 PASS** |
| `RateLimitFilterTest`                  |   **6/6 PASS** |
| `WithdrawalServiceIntegrationTest`     | **17/17 PASS** |
| `WithdrawalConcurrencyIntegrationTest` |   **1/1 PASS** |

The aggregate full-project regression count should always be refreshed using:

```bash
mvn clean test
```

Do not treat an old aggregate number as the current baseline after adding new tests.

---

# 8. Backend Security Status

Current security implementation:

| Security Requirement                     | Status    |
| ---------------------------------------- | --------- |
| JWT authentication                       | COMPLETED |
| Protected APIs                           | COMPLETED |
| User authorization                       | COMPLETED |
| Ownership protection                     | COMPLETED |
| Server-side validation                   | COMPLETED |
| Balance validation                       | COMPLETED |
| Payout validation                        | COMPLETED |
| Withdrawal eligibility                   | COMPLETED |
| Idempotency                              | COMPLETED |
| Concurrent withdrawal protection         | COMPLETED |
| Audit logging                            | COMPLETED |
| Rate limiting                            | COMPLETED |
| Method-specific payout detail validation | COMPLETED |
| Security integration testing             | COMPLETED |
| Advanced fraud detection                 | FUTURE    |
| Advanced reconciliation                  | FUTURE    |

---

# 9. Backend Source of Truth

The backend owns:

```text
Wallet balance
Payout amount
Required VEs
Payout method
Payout option
Withdrawal amount
Withdrawal status
Eligibility
Transaction status
```

The frontend only displays backend-controlled information.

---

# 10. Wallet Architecture

Conceptually:

```text
User
│
├── userId
├── email
├── name
├── accountStatus
├── verified
└── Wallet
      │
      ├── VEs
      ├── SVEs
      ├── Gems
      ├── Tokens
      ├── Spins
      ├── createdAt
      └── updatedAt
```

---

# 11. Wallet Ledger

The wallet does not rely only on the current balance.

Every important wallet mutation must be explainable through transaction history.

Conceptual transaction structure:

```text
WalletTransaction
├── transactionId
├── userId
├── currency
├── type
├── amount
├── balanceBefore
├── balanceAfter
├── source
├── referenceId
├── status
├── description
├── metadata
├── createdAt
└── updatedAt
```

Example:

```text
Balance Before: 10,000 VEs
Credit:          +500 VEs
Balance After:  10,500 VEs
Source:          WATCH_AD
```

Withdrawal:

```text
Balance Before: 10,500 VEs
Debit:          -2,400 VEs
Balance After:   8,100 VEs
Source:          WITHDRAWAL
Reference:       withdrawalId
```

---

# 12. Transaction Types

## Credit

Examples:

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

Examples:

```text
WITHDRAWAL
EXCHANGE_DEBIT
ADMIN_DEBIT
CORRECTION
```

Additional transaction types may be added only when genuinely required.

---

# 13. Wallet APIs

Current architecture supports wallet-related APIs including:

```text
GET /api/wallet
GET /api/wallet/transactions
GET /api/wallet/summary
POST /api/wallet/credit
POST /api/wallet/debit
```

Sensitive mutation APIs must not be publicly accessible to normal users.

---

# 14. Wallet Service

Wallet logic must remain in services rather than being duplicated inside controllers/routes.

Responsibilities include:

```text
creditWallet()
debitWallet()
getWallet()
getTransactions()
validateBalance()
createLedgerEntry()
```

The wallet service can later be reused by:

```text
Watch Ads
Referrals
Daily Rewards
Games
Spins
Mining
Exchanges
Withdrawals
Admin Operations
```

---

# 15. Payout Architecture

Payout configuration is backend-controlled.

Conceptually:

```text
PayoutMethod
├── methodId
├── name
├── type
├── currency
├── active
├── eligibility
└── metadata

PayoutOption
├── optionId
├── payoutMethod
├── payoutValue
├── requiredAmount
├── currency
├── active
└── metadata
```

The frontend must not hardcode payout values.

Incorrect:

```javascript
const payoutOptions = [
    { amount: 10 },
    { amount: 25 },
    { amount: 50 }
];
```

Correct:

```text
Frontend
   ↓
GET payout configuration
   ↓
Backend
   ↓
Database configuration
```

---

# 16. Current UPI Payout Configuration

Current configured reference values:

| Redemption | Required VEs |
| ---------- | -----------: |
| ₹10        |        2,400 |
| ₹25        |        5,800 |
| ₹50        |       10,000 |
| ₹100       |       19,500 |
| ₹150       |       28,500 |
| ₹300       |       52,500 |
| ₹500       |       80,500 |
| ₹1,000     |      150,000 |

These values are backend-controlled.

If the actual live/current VELoop payout configuration changes, the backend configuration must be updated instead of putting new values into frontend code.

---

# 17. Withdrawal Architecture

Withdrawal flow:

```text
Wallet
   ↓
Choose Withdrawal / Redeem
   ↓
Payout Method
   ↓
Payout Option
   ↓
Required Details
   ↓
Confirmation
   ↓
POST Withdrawal
   ↓
Server Validation
   ↓
Balance Validation
   ↓
Wallet Deduction
   ↓
Ledger Entry
   ↓
Withdrawal Record
   ↓
PENDING
```

---

# 18. Withdrawal Model

Conceptual structure:

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

---

# 19. Withdrawal Status

Supported statuses:

```text
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
```

Example:

```text
User Request
     ↓
  PENDING
     ↓
PROCESSING
     ↓
APPROVED
```

Or:

```text
PENDING
   ↓
REJECTED
```

The system preserves withdrawal history.

---

# 20. Wallet Deduction Strategy

The current architecture uses immediate wallet deduction when a valid withdrawal is created.

Example:

```text
Available VEs
25,000

Withdrawal
19,500

Remaining
5,500
```

The withdrawal and wallet mutation must be treated as one logically consistent financial operation.

The system must never create:

```text
Withdrawal exists
+
Wallet still contains deducted amount
```

for a successful withdrawal.

If a withdrawal is rejected after deduction, the system must use the established reversal mechanism so the final wallet state remains correct.

---

# 21. Atomicity & Concurrency

Critical operations must protect against race conditions.

Example:

```text
Balance = 10,000 VEs

Request A = 8,000
Request B = 8,000
```

The system must not allow both to successfully consume the same balance.

Required logical sequence:

```text
Validate request
      ↓
Validate eligibility
      ↓
Validate payout configuration
      ↓
Check balance
      ↓
Deduct wallet
      ↓
Create ledger
      ↓
Create withdrawal
```

If a critical operation fails, the system must not leave an inconsistent financial state.

---

# 22. Idempotency

Every withdrawal creation requires an idempotency key.

Example:

```text
POST /api/withdrawals
Idempotency-Key: abc123
```

If the same request is submitted twice:

```text
Request 1 → Withdrawal W001
Request 2 → Existing W001 returned
```

The wallet is deducted only once.

If:

```text
Idempotency-Key = abc123
```

is reused with different request data:

```text
409 Conflict
```

This protects against:

* Double-clicks
* Browser retries
* Network retries
* Client-side duplicate submissions
* Concurrent duplicate requests

---

# 23. Balance Validation

Every debit operation validates the server-side balance.

Example:

```text
Required = 2,400 VEs
Balance  = 1,900 VEs
```

Result:

```text
Rejected
```

The frontend cannot override the balance.

The backend determines:

```text
Current balance
Required amount
Actual payout amount
Currency
Eligibility
```

---

# 24. Payout Detail Validation

The backend validates payout details according to payout method.

For UPI:

```text
UPI ID
```

is validated server-side.

Additional rules include:

```text
Required
Non-blank
Maximum length
Format validation
Whitespace normalization
```

The frontend confirmation screen is not a security mechanism.

---

# 25. Withdrawal Eligibility

Current eligibility rules:

```text
User exists
AND
Account Status = ACTIVE
AND
Verified = true
```

Only then can a withdrawal proceed.

Future advanced eligibility may include:

```text
Fraud detection
Velocity checks
Device risk
Suspicious activity detection
KYC/verification rules
Account age
Withdrawal history analysis
```

---

# 26. Authentication & Authorization

Wallet APIs require authentication.

Conceptual flow:

```text
JWT
 ↓
Authentication Filter
 ↓
Authenticated User
 ↓
Controller
 ↓
Service
 ↓
User's Wallet
```

A user must not be able to access another user's wallet by modifying:

```text
userId
```

in a request.

Example attack:

```text
GET /api/wallet?userId=someoneElse
```

must not expose another user's wallet.

---

# 27. Rate Limiting

Sensitive endpoints are rate limited.

Current categories include:

```text
Login
Withdrawal
Wallet mutations
Withdrawal mutations
```

The purpose is to reduce:

* Brute-force attempts
* Repeated submissions
* Abuse
* Accidental request storms
* Automated attacks

Rate limiting is already implemented and tested.

---

# 28. Input Validation

Server-side validation exists for important financial inputs.

Validation includes:

```text
Currency
Amount
Payout method
Payout option
Payout details
User status
Withdrawal request
Idempotency key
```

The backend does not trust client-provided financial values.

---

# 29. Audit Logging

Audit logging is implemented and manually verified.

Important events:

```text
WITHDRAWAL_CREATED
WITHDRAWAL_PROCESSING
WITHDRAWAL_APPROVED
WITHDRAWAL_REJECTED
WITHDRAWAL_CANCELLED
```

The audit model contains:

```text
actor
targetUser
targetType
action
referenceId
metadata
createdAt
```

Future improvements may include richer request context such as:

```text
IP address
Session information
Device information
Request correlation ID
```

where appropriate.

---

# 30. Database Indexing

The architecture considers indexing for frequently queried fields including:

```text
userId
transactionId
withdrawalId
referenceId
createdAt
status
```

Indexes are important for:

* Wallet history
* Withdrawal history
* Audit searches
* User-specific queries
* Large transaction volumes

---

# 31. Data Consistency Rule

The wallet must always be explainable.

Conceptually:

```text
Current Wallet Balance
=
Initial Balance
+ Credits
- Debits
± Adjustments
```

The ledger should allow developers to investigate how the current balance was reached.

---

# 32. Ownership Protection

Every authenticated wallet operation must operate on the authenticated user.

The backend must not trust:

```text
userId
```

from arbitrary client input when the authenticated identity is already available.

The server determines:

```text
Authenticated User
        ↓
User Wallet
        ↓
User Transactions
        ↓
User Withdrawals
```

---

# 33. API Error Handling

Expected errors include:

### Insufficient Balance

```text
Insufficient VEs balance.
```

### Invalid Payout Option

```text
Selected payout option is unavailable.
```

### Inactive Payout Method

```text
This payout method is currently unavailable.
```

### Duplicate Request

```text
This withdrawal request has already been submitted.
```

### Authentication

```text
Authentication required.
```

### Invalid UPI

```text
Invalid UPI ID
```

### Ineligible User

```text
User account is not eligible for withdrawal
```

Internal database exceptions should not be exposed directly to users.

---

# 34. Current Backend Test Coverage

Important verified suites:

```text
PayoutDetailValidatorTest
10/10 PASS

WithdrawalEligibilityServiceTest
6/6 PASS

RateLimitServiceTest
8/8 PASS

RateLimitFilterTest
6/6 PASS

WithdrawalServiceIntegrationTest
17/17 PASS

WithdrawalConcurrencyIntegrationTest
1/1 PASS

WithdrawalApiIntegrationTest
14/14 PASS
```

Before starting a new major phase:

```bash
mvn clean test
```

should be executed.

The resulting aggregate test count should be recorded in the checkpoint after verification.

---

# 35. Important Real-World Test Scenarios

The system must support the following scenarios.

## Test 1 — Normal Credit

```text
1000 VEs
+
500 VEs
=
1500 VEs
```

---

## Test 2 — Normal Withdrawal

```text
1500 VEs
-
1000 VEs
=
500 VEs
```

---

## Test 3 — Insufficient Balance

```text
500 VEs
withdraw 1000 VEs
```

Expected:

```text
Rejected
```

---

## Test 4 — Double Click

```text
Request A
Request A
```

Expected:

```text
One withdrawal
One wallet deduction
```

---

## Test 5 — Concurrent Withdrawal

```text
Balance = 10,000

Request A = 8,000
Request B = 8,000
```

Expected:

```text
Both cannot succeed.
```

---

## Test 6 — Invalid Payout Option

```text
Fake option ID
```

Expected:

```text
Rejected
```

---

## Test 7 — Another User's Wallet

```text
User A
   ↓
Attempts User B wallet
```

Expected:

```text
Rejected
```

---

## Test 8 — Rejected Withdrawal

A rejected withdrawal must correctly restore/release the wallet amount according to the selected wallet deduction strategy.

---

## Test 9 — API Manipulation

Example frontend request:

```json
{
  "amount": 1000000
}
```

The backend must ignore untrusted financial values and calculate/validate the actual payout configuration.

---

# 36. Demonstration Frontend

Status:

```text
PENDING
```

The demonstration frontend is mandatory because it proves that the backend actually works.

Required routes:

```text
/wallet
/payout
```

Optional:

```text
/login
```

The frontend is not the source of truth.

---

# 37. Phase 10 — Demonstration Frontend

## Status

```text
NEXT PENDING PHASE
```

## Objective

Build a small React demonstration frontend connected to the completed backend.

Do not spend excessive time on UI design.

Backend correctness remains the primary objective.

---

## Phase 10.1 — Wallet Page

Implement:

```text
/wallet
```

The page must:

1. Fetch wallet data from backend.
2. Display relevant wallet balances.
3. Fetch wallet transactions.
4. Display transaction information.
5. Show withdrawal/redeem action.
6. Navigate to `/payout`.
7. Show loading state.
8. Show error state.
9. Refresh wallet after successful withdrawal.

---

## Phase 10.2 — Payout Page

Implement:

```text
/payout
```

Flow:

```text
Fetch payout methods
        ↓
Select method
        ↓
Fetch/show payout options
        ↓
Select option
        ↓
Show required details
        ↓
Enter payout details
        ↓
Confirmation
        ↓
POST /api/withdrawals
```

---

## Phase 10.3 — Backend-Driven UI

The frontend must not contain hardcoded financial rules.

Do not implement:

```javascript
const requiredVEs = 19500;
```

Instead:

```text
Frontend
   ↓
Payout API
   ↓
Backend configuration
   ↓
Required VEs
```

---

## Phase 10.4 — Confirmation

Before withdrawal:

```text
Are you sure you want to redeem this reward?

Payout: ₹100
Required: 19,500 VEs

[Cancel] [Confirm]
```

The backend must perform final validation after confirmation.

---

## Phase 10.5 — Successful Withdrawal

After success:

```text
Withdrawal submitted successfully.

Status: PENDING
```

Then:

```text
Refresh wallet
Refresh transaction history
```

---

# 38. Phase 11 — API Documentation & Postman

Status:

```text
PENDING
```

Required:

```text
API_DOCUMENTATION.md
Postman Collection
```

Documentation should cover:

```text
Endpoint
HTTP Method
Authentication
Request
Response
Validation
Errors
Example
```

Major APIs include:

```text
GET /api/wallet

GET /api/wallet/transactions

GET /api/wallet/summary

GET /api/payout/methods

GET /api/payout/options/:method

POST /api/withdrawals

GET /api/withdrawals

GET /api/withdrawals/:id
```

---

# 39. Phase 12 — Final Regression & Documentation

Status:

```text
PENDING
```

Tasks:

```text
Run mvn clean test
Verify all backend tests
Record aggregate result
Verify Swagger
Verify wallet flow
Verify payout flow
Verify withdrawal flow
Verify error handling
Update README
Update API documentation
Prepare Postman collection
Document database architecture
Document security architecture
```

---

# 40. Phase 13 — Deployment & Final Demonstration

Status:

```text
PENDING
```

Possible deployment architecture:

```text
React Frontend
      ↓
Vercel / Netlify
      ↓
Backend API
      ↓
Render / Railway / Suitable Platform
      ↓
MongoDB Atlas / Development Database
```

Only the intern's own demonstration database must be used.

Never connect internship development code to VELoop production credentials.

---

# 41. Production Safety

This project must not use production credentials.

Development architecture:

```text
Intern Local Frontend
        ↓
Intern Local Backend
        ↓
Intern MongoDB
```

Not:

```text
Intern
  ↓
VELoop Production Database
```

The authorized development team can later integrate selected code into the actual VELoop system.

---

# 42. Environment Variables

Sensitive values must never be committed.

Example:

```env
PORT=
MONGO_URI=
JWT_SECRET=
```

Commit:

```text
.env.example
```

Never commit:

```text
.env
```

with real credentials.

Never commit:

```text
Database passwords
JWT secrets
API keys
Production credentials
```

---

# 43. Suggested Project Structure

The current project may use a structure appropriate to the actual implementation.

Conceptual target:

```text
backend/
│
├── src/
│   ├── config/
│   ├── controllers/
│   ├── middleware/
│   ├── models/
│   ├── routes/
│   ├── services/
│   ├── utils/
│   ├── validators/
│   ├── migrations/
│   └── app.js
│
├── seed/
│   └── payoutOptions.js
│
├── .env.example
├── package.json
└── README.md

frontend/
│
├── src/
├── public/
├── package.json
└── README.md
```

The architecture can be improved if required.

---

# 44. Recommended Models

Minimum logical models:

```text
User
Wallet
WalletTransaction
Withdrawal
PayoutMethod
PayoutOption
AuditLog
```

Additional models may be introduced when genuinely required.

---

# 45. Seed Data

Demonstration data should be clearly marked as test/demo data.

Example:

```text
User:
demo@veloop.test
```

Example wallet:

```text
VEs:    25,000
SVEs:    5,000
Gems:      100
Tokens:    500
Spins:       3
```

Example transactions:

```text
Ad Reward
Daily Reward
Referral Reward
Bonus
Withdrawal
```

Do not present demonstration data as real production user data.

---

# 46. Current Architecture

Current backend architecture can be summarized as:

```text
                    ┌────────────────────┐
                    │   React Frontend   │
                    │   Phase 10         │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │    REST APIs       │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Authentication     │
                    │ JWT                │
                    │ Rate Limiting      │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Controllers        │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Services           │
                    │                    │
                    │ Wallet             │
                    │ Payout             │
                    │ Withdrawal         │
                    │ Idempotency        │
                    │ Eligibility        │
                    │ Audit              │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Database           │
                    │                    │
                    │ User               │
                    │ Wallet             │
                    │ Ledger             │
                    │ Payout             │
                    │ Withdrawal         │
                    │ Audit              │
                    └────────────────────┘
```

---

# 47. Completed Architecture Layers

The following backend layers are already implemented:

```text
Authentication
        ↓
Authorization
        ↓
Validation
        ↓
Wallet
        ↓
Ledger
        ↓
Payout
        ↓
Withdrawal
        ↓
Idempotency
        ↓
Concurrency
        ↓
Eligibility
        ↓
Rate Limiting
        ↓
Audit Logging
```

---

# 48. What NOT To Rebuild

Do not unnecessarily rebuild:

```text
Wallet core
Wallet ledger
Wallet balance validation
Payout configuration
Withdrawal service
Withdrawal statuses
Idempotency
Concurrency protection
Audit logging
JWT authentication
Authorization
Rate limiting
Payout detail validation
Withdrawal eligibility
Security integration tests
```

These are already implemented and verified.

If a future phase requires changing one of them, first inspect the existing implementation and tests.

---

# 49. Remaining Project Work

After Phase 9, remaining major work is:

```text
Phase 10
Demonstration Frontend
        ↓
Phase 11
API Documentation + Postman
        ↓
Phase 12
Final Regression + Documentation
        ↓
Phase 13
Deployment + Final Demonstration
        ↓
Future
Monitoring / Reconciliation / Advanced Fraud / Scalability
```

---

# 50. Advanced Future Improvements

These are not required to reopen completed phases.

Possible future improvements:

```text
Advanced fraud detection
Withdrawal velocity rules
Risk scoring
Queue-based withdrawal processing
Distributed rate limiting
Redis
Caching
Event-driven wallet architecture
Reconciliation jobs
Monitoring
Metrics
Alerting
Distributed tracing
Database sharding
Read replicas
High-volume ledger optimization
Financial reconciliation
```

---

# 51. Scalability Challenge

The final README must eventually answer:

> If VELoop Rewards grows from 1,000 users to 1,000,000 users, what changes would you make to this wallet architecture to maintain balance accuracy, transaction consistency, performance, security and withdrawal reliability?

The answer should discuss:

```text
Database transactions
Atomic updates
Ledger architecture
Idempotency
Database indexing
Queues
Caching
Rate limiting
Fraud detection
Audit logs
Reconciliation
Monitoring
Scalability
```

---

# 52. Evaluation Priority

The original task evaluation priorities are:

| Area                   | Weight |
| ---------------------- | -----: |
| Backend architecture   |    20% |
| Wallet correctness     |    20% |
| Security               |    15% |
| Withdrawal system      |    15% |
| Database design        |    10% |
| Edge cases             |    10% |
| API quality            |     5% |
| Demonstration frontend |     5% |

The backend is therefore the primary evaluation area.

---

# 53. Final Deliverables

Required deliverables:

```text
GitHub Repository
Live Frontend Link
Live Backend/API Link
README.md
API_DOCUMENTATION.md
Database/Model Documentation
Postman Collection
Test Cases/Results
.env.example
Architecture Explanation
Short Demonstration Video
```

---

# 54. GitHub Repository Requirements

Repository should eventually contain:

```text
/backend
/frontend
README.md
API_DOCUMENTATION.md
.env.example
Postman Collection
Documentation
```

Do not commit:

```text
.env
Passwords
JWT secrets
MongoDB credentials
API keys
Production credentials
```

---

# 55. Final Demonstration Flow

The final demonstration must show:

```text
Login / Demo User
        ↓
Wallet
        ↓
Backend Fetches Balances
        ↓
Transactions Loaded
        ↓
Choose Withdrawal
        ↓
Payout Page
        ↓
Payout Methods Loaded From Backend
        ↓
Select Payout Option
        ↓
Required VEs Calculated By Backend
        ↓
Enter Payout Details
        ↓
Confirmation
        ↓
POST Withdrawal
        ↓
Server Validates Everything
        ↓
Wallet Transaction Created
        ↓
Balance Safely Deducted
        ↓
Withdrawal = PENDING
        ↓
Wallet Refreshed
        ↓
Transaction History Updated
```

---

# 56. Security Demonstration

The final demonstration should also prove:

### Frontend Manipulation

Attempt:

```text
amount = 1000000
```

Expected:

```text
Backend rejects invalid request
```

### Wallet Manipulation

Attempt:

```text
VEs = 999999
```

Expected:

```text
Actual backend balance remains unchanged
```

### Unauthorized Wallet Access

Attempt:

```text
User A → User B wallet
```

Expected:

```text
Rejected
```

### Duplicate Withdrawal

Attempt:

```text
Same Idempotency-Key
Same Request
```

Expected:

```text
One withdrawal
One deduction
```

### Same Key / Different Request

Expected:

```text
409 Conflict
```

### Concurrent Withdrawal

Expected:

```text
Balance cannot be consumed twice
```

---

# 57. Submission Checklist

## Wallet

* [x] Wallet is backend-driven
* [x] VEs are server-authoritative
* [x] Other wallet currencies are server-authoritative
* [x] Wallet transactions are stored
* [x] Credits create ledger records
* [x] Debits create ledger records
* [x] Balance validation implemented

## Payout

* [x] Payout configuration is backend-controlled
* [x] Payout values are not trusted from frontend
* [x] Payout method validation implemented
* [x] Payout option validation implemented
* [x] Payout detail validation implemented

## Withdrawal

* [x] Withdrawal requests stored
* [x] Withdrawal statuses implemented
* [x] Insufficient balance rejected
* [x] Duplicate withdrawal protected
* [x] Concurrent withdrawal protected
* [x] Wallet deduction implemented
* [x] Rejection/reversal handling implemented
* [x] Cancellation handling implemented

## Security

* [x] Authentication implemented
* [x] JWT validation implemented
* [x] Protected APIs
* [x] Authorization
* [x] Ownership protection
* [x] Server-side validation
* [x] Rate limiting
* [x] Idempotency
* [x] Withdrawal eligibility
* [x] Audit logging
* [x] Security integration tests

## Testing

* [x] Payout detail validation tests
* [x] Eligibility tests
* [x] Rate-limit service tests
* [x] Rate-limit filter tests
* [x] Withdrawal integration tests
* [x] Withdrawal concurrency test
* [x] HTTP withdrawal security tests
* [ ] Final `mvn clean test` aggregate baseline refresh

## Frontend

* [ ] `/wallet`
* [ ] `/payout`
* [ ] Backend-driven wallet display
* [ ] Backend-driven payout selection
* [ ] Withdrawal confirmation
* [ ] Wallet refresh
* [ ] Loading/error states

## Documentation

* [x] README architecture/checkpoint
* [ ] API_DOCUMENTATION.md
* [ ] Database/model documentation
* [ ] Postman collection
* [ ] Test-case documentation
* [ ] Architecture explanation
* [ ] Scalability explanation

## Deployment

* [ ] Frontend deployment
* [ ] Backend deployment
* [ ] Demonstration database
* [ ] Live API
* [ ] Live frontend
* [ ] Final demonstration

## GitHub

* [ ] Final repository cleanup
* [ ] `.env` excluded
* [x] `.env.example` strategy defined
* [ ] Documentation committed
* [ ] Postman collection committed
* [ ] Final project submitted

---

# 58. Current Checkpoint

```text
============================================================
BACKEND-CHECKPOINT-03
SECURITY HARDENING VERIFIED
============================================================

Completed:

[✓] Project Setup
[✓] Database
[✓] Flyway
[✓] Authentication
[✓] JWT
[✓] Authorization
[✓] OpenAPI / Swagger
[✓] Wallet
[✓] Wallet Ledger
[✓] Wallet Balance Validation
[✓] Wallet Transactions
[✓] Payout Configuration
[✓] Payout Methods
[✓] Payout Options
[✓] Withdrawal
[✓] Withdrawal Status
[✓] Wallet Deduction
[✓] Rejection/Reversal
[✓] Cancellation/Reversal
[✓] Idempotency
[✓] Concurrency Protection
[✓] Audit Logging
[✓] Rate Limiting
[✓] Payout Detail Validation
[✓] Withdrawal Eligibility
[✓] Security Integration Testing

Verified Phase 9 tests:

WithdrawalApiIntegrationTest              14/14 PASS
PayoutDetailValidatorTest                 10/10 PASS
WithdrawalEligibilityServiceTest           6/6 PASS
RateLimitServiceTest                       8/8 PASS
RateLimitFilterTest                        6/6 PASS
WithdrawalServiceIntegrationTest          17/17 PASS
WithdrawalConcurrencyIntegrationTest       1/1 PASS

============================================================
NEXT:
PHASE-10 — DEMONSTRATION FRONTEND
============================================================
```

---

# 59. Exact Next Starting Point

When development resumes, **do not inspect the entire project again**.

Start from this exact sequence:

### Step 1

Run:

```bash
mvn clean test
```

Record the current aggregate test result.

---

### Step 2

Do not modify completed Phase 1–9 modules unless a test exposes an actual regression.

---

### Step 3

Start:

```text
PHASE-10 — DEMONSTRATION FRONTEND
```

First implement:

```text
/wallet
```

Then:

```text
/payout
```

---

### Step 4

Verify the frontend uses backend APIs for:

```text
Wallet balance
Transactions
Payout methods
Payout options
Required VEs
Withdrawal
Withdrawal status
```

---

### Step 5

After Phase 10:

```text
Run tests
Verify API integration
Verify frontend flow
Update README checkpoint
```

---

### Step 6

Continue to:

```text
PHASE-11 — API DOCUMENTATION + POSTMAN
```

---

# 60. Phase Completion Rule

Every future phase must follow:

```text
Implementation
      ↓
Unit Tests
      ↓
Integration Tests
      ↓
Manual Verification
      ↓
Regression Test
      ↓
Git Commit
      ↓
README Update
      ↓
Checkpoint
      ↓
Next Phase
```

A phase is not considered complete only because the code compiles.

It must be:

```text
Implemented
+
Tested
+
Verified
+
Documented
```

---

# 61. Resume Instructions

If this project is resumed after a long break, read only these sections first:

```text
Section 5   — Current Development Status
Section 7   — Phase 9 Verification Baseline
Section 8   — Backend Security Status
Section 39  — Remaining Project Work
Section 58  — Current Checkpoint
Section 59  — Exact Next Starting Point
```

This README is intended to act as the project's development checkpoint so that the entire codebase does not need to be re-understood from scratch before continuing.

---

# 62. Final Project Principle

The most important rule remains:

> **The frontend is only the interface. The backend owns the financial/reward logic.**

If someone opens DevTools and changes:

```text
amount = 100
```

to:

```text
amount = 100000
```

the backend must still validate the actual payout configuration.

If someone changes:

```text
VEs = 999999
```

in browser storage, the actual wallet balance must not change.

The backend must always determine:

```text
Current Balance
Required VEs
Payout Amount
Payout Method
Payout Option
Eligibility
Withdrawal Amount
Transaction Status
```

Therefore:

```text
Frontend
   ↓
Request
   ↓
Backend
   ↓
Validation
   ↓
Database
   ↓
Ledger
   ↓
Auditable Financial State
```

---

# 63. Final Current Status

```text
============================================================
VELoop Rewards Wallet & Withdrawal System
============================================================

CORE BACKEND
[COMPLETED]

WALLET
[COMPLETED]

LEDGER
[COMPLETED]

PAYOUT CONFIGURATION
[COMPLETED]

WITHDRAWAL
[COMPLETED]

IDEMPOTENCY
[COMPLETED]

CONCURRENCY
[COMPLETED]

AUDIT LOGGING
[COMPLETED + VERIFIED]

SECURITY HARDENING
[COMPLETED + VERIFIED]

RATE LIMITING
[COMPLETED + VERIFIED]

PAYOUT DETAIL VALIDATION
[COMPLETED + VERIFIED]

WITHDRAWAL ELIGIBILITY
[COMPLETED + VERIFIED]

HTTP SECURITY TESTING
[COMPLETED — 14/14 PASS]

------------------------------------------------------------

NEXT DEVELOPMENT AREA

PHASE-10
DEMONSTRATION FRONTEND

/wallet
/payout

------------------------------------------------------------

AFTER PHASE 10

PHASE-11
API DOCUMENTATION + POSTMAN

PHASE-12
FINAL REGRESSION + DOCUMENTATION

PHASE-13
DEPLOYMENT + FINAL DEMONSTRATION

------------------------------------------------------------

FUTURE

ADVANCED FRAUD DETECTION
RECONCILIATION
MONITORING
SCALABILITY
HIGH-VOLUME PROCESSING
------------------------------------------------------------
```

**Current resume point:** `PHASE-10 — DEMONSTRATION FRONTEND`
