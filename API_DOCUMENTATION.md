# VELoop Rewards Backend API Documentation

## 1. Overview

VELoop Rewards Backend is a Spring Boot REST API for a backend-controlled rewards wallet, payout configuration, withdrawal processing, security, audit logging, fraud protection, reconciliation, rate limiting, and observability.

The backend is the source of truth for:

- User identity and authentication
- Wallet balances
- Wallet transactions
- Payout configuration
- Withdrawal amounts
- Withdrawal status
- Withdrawal eligibility
- Idempotency
- Authorization
- Audit records
- Fraud and abuse controls
- Reconciliation

The client must not calculate or directly modify financial state.

---

# 2. API Base URL

For local development:

```text
http://localhost:8080
```

API prefix:

```text
/api
```

Example:

```text
GET http://localhost:8080/api/wallet
```

---

# 3. Authentication

Protected endpoints use JWT Bearer authentication.

Header:

```http
Authorization: Bearer <JWT>
```

Authentication flow:

```text
Client
   |
   | Authorization: Bearer <JWT>
   v
JwtAuthenticationFilter
   |
   v
JWT validation
   |
   v
Authenticated user + role
   |
   v
SecurityContext
   |
   v
Controller
```

The backend determines the authenticated user from the security context.

Clients must not use a request parameter such as:

```text
/api/wallet?userId=anotherUser
```

to access another user's wallet.

---

# 4. Authorization Model

The API uses authenticated user identity and role-based authorization.

## USER

A normal authenticated user can:

- View their own wallet
- View their own wallet summary
- View their own transactions
- View payout configuration
- Create their own withdrawal
- View their own withdrawals
- View their own withdrawal details
- Cancel an eligible withdrawal

## ADMIN

Administrative operations include:

- Wallet credit
- Wallet debit
- Withdrawal processing
- Withdrawal approval
- Withdrawal rejection
- Administrative withdrawal management
- Reconciliation

Users cannot perform administrative wallet mutations.

Users cannot access another user's wallet or withdrawal records.

---

# 5. Authentication APIs

## 5.1 Register

```http
POST /api/auth/register
```

### Authentication

Public.

### Authorization

None.

### Request

```json
{
  "name": "Demo User",
  "email": "demo@example.com",
  "password": "Password@123"
}
```

### Validation

The backend validates:

- Name
- Email
- Password
- Duplicate email

Passwords are hashed and are never stored as plaintext.

### Success

```text
201 Created
```

---

## 5.2 Login

```http
POST /api/auth/login
```

### Authentication

Public.

### Authorization

None.

### Request

```json
{
  "email": "demo@example.com",
  "password": "Password@123"
}
```

### Success

```text
200 OK
```

Example response:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": 1,
    "email": "demo@example.com",
    "name": "Demo User",
    "role": "USER",
    "token": "<JWT>"
  }
}
```

---

## 5.3 Current User

```http
GET /api/auth/me
```

### Authentication

Required.

```http
Authorization: Bearer <JWT>
```

### Authorization

Authenticated user.

### Purpose

Returns the currently authenticated user's identity.

The backend obtains the user from the authenticated security context rather than trusting a client-supplied user ID.

---

# 6. Wallet APIs

## 6.1 Get Wallet

```http
GET /api/wallet
```

### Authentication

Required.

### Authorization

Authenticated user.

### Request

No request body.

The backend determines the wallet owner from the authenticated JWT.

### Supported currencies

```text
VES
SVES
GEMS
TOKENS
SPINS
```

### Conceptual response

```json
{
  "success": true,
  "message": "Wallet retrieved successfully",
  "data": {
    "ves": 25000.0000,
    "sves": 5000.0000,
    "gems": 100.0000,
    "tokens": 500.0000,
    "spins": 3.0000
  }
}
```

The actual values come from the database.

---

## 6.2 Get Wallet Transactions

```http
GET /api/wallet/transactions
```

### Authentication

Required.

### Authorization

Authenticated user.

### Pagination

```http
GET /api/wallet/transactions?page=1&limit=20
```

Only transactions belonging to the authenticated user are returned.

### Ledger fields

A transaction contains fields such as:

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

### Conceptual response

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

---

## 6.3 Get Wallet Summary

```http
GET /api/wallet/summary
```

### Authentication

Required.

### Authorization

Authenticated user.

### Response

The summary contains current balances and transaction summary information.

Example:

```json
{
  "success": true,
  "message": "Wallet summary retrieved successfully",
  "data": {
    "ves": 25000.0000,
    "sves": 5000.0000,
    "gems": 100.0000,
    "tokens": 500.0000,
    "spins": 3.0000,
    "totalTransactions": 10
  }
}
```

---

# 7. Administrative Wallet APIs

These operations are protected administrative operations.

## 7.1 Credit Wallet

```http
POST /api/wallet/credit
```

### Authentication

Required.

### Authorization

Admin authorization required.

### Request

The request does not contain a user ID. The backend derives the wallet owner from the authenticated principal.

```json
{
  "currency": "VES",
  "amount": 1000,
  "transactionType": "ADMIN_CREDIT",
  "source": "ADMIN",
  "referenceId": "ADMIN-CREDIT-001",
  "description": "Administrative wallet credit",
  "metadata": null
}
```

### Backend behavior

```text
Validate request
      ↓
Load wallet
      ↓
Apply credit
      ↓
Persist wallet
      ↓
Create ledger transaction
      ↓
Audit sensitive operation
```

The client cannot set the final wallet balance.

---

## 7.2 Debit Wallet

```http
POST /api/wallet/debit
```

### Authentication

Required.

### Authorization

Admin authorization required.

### Request

```json
{
  "currency": "VES",
  "amount": 1000,
  "transactionType": "ADMIN_DEBIT",
  "source": "ADMIN",
  "referenceId": "ADMIN-DEBIT-001",
  "description": "Administrative wallet debit",
  "metadata": null
}
```

### Validation

The backend validates:

- Currency
- Amount
- Wallet
- Available balance
- Transaction type

Insufficient balance is rejected.

---

# 8. Payout Configuration API

## 8.1 Get Payout Configuration

```http
GET /api/payouts/configuration
```

### Authentication

Required.

### Authorization

Authenticated user.

### Response

The backend returns active payout methods and options.

Example:

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

Inactive payout methods/options are excluded.

The frontend must not hardcode payout values.

---

# 9. Withdrawal APIs

## 9.1 Create Withdrawal

```http
POST /api/withdrawals
```

### Authentication

Required.

```http
Authorization: Bearer <JWT>
```

### Required header

```http
Idempotency-Key: <unique-key>
```

The controller accepts the header as optional at HTTP binding level, but the service rejects a missing or blank key. Clients must therefore always send it.

Maximum key length:

```text
100 characters
```

### Request

```json
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "demo@veloop.test"
}
```

### Important

The client does not provide the authoritative VES amount.

The backend resolves:

```text
Payout Method
       ↓
Payout Option
       ↓
Payout Amount
       ↓
Currency
       ↓
Required VES
       ↓
Wallet operation
```

### Validation

The backend validates:

- Idempotency key
- Request fingerprint
- User existence
- Withdrawal eligibility
- Payout method existence
- Payout method activity
- Payout option existence
- Payout option activity
- Method/option relationship
- Payout details
- Required VES balance
- Fraud/risk decision

### Normal ALLOW path

```text
Request
   ↓
Validation
   ↓
Fraud ALLOW
   ↓
Resolve payout values
   ↓
Debit VES
   ↓
Create WITHDRAWAL ledger transaction
   ↓
Create PENDING withdrawal
   ↓
Audit + idempotency
```

---

## 9.2 Get Withdrawal History

```http
GET /api/withdrawals?page=1&limit=20
```

### Authentication

Required.

### Authorization

Authenticated user.

Only withdrawals belonging to the authenticated user are returned.

---

## 9.3 Get Withdrawal Details

```http
GET /api/withdrawals/{withdrawalId}
```

### Authentication

Required.

### Authorization

Authenticated user.

### Ownership

The withdrawal must belong to the authenticated user.

Access to another user's withdrawal is rejected.

---

## 9.4 Process Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/processing
```

### Authentication

Required.

### Authorization

Admin.

### Transition

```text
PENDING → PROCESSING
```

Only eligible `PENDING` withdrawals can be moved to `PROCESSING`.

---

## 9.5 Approve Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/approve
```

### Authentication

Required.

### Authorization

Admin.

Approval follows the implemented state guards.

For a normally debited withdrawal, approval does not perform a second wallet debit.

For a REVIEW withdrawal that has no linked financial transaction, approval performs the required VES debit before approval.

---

## 9.6 Reject Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/reject?rejectionReason=...&reviewNote=...
```

### Authentication

Required.

### Authorization

Admin.

### Required parameter

```text
rejectionReason
```

### Allowed states

```text
PENDING
PROCESSING
```

### Financial behavior

A normally debited withdrawal is reversed using a compensating `CORRECTION` credit.

The original debit remains in the ledger.

---

## 9.7 Cancel Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
```

### Authentication

Required.

### Authorization

Authenticated `USER` role + owner.

### Allowed state

```text
PENDING
```

### Financial behavior

For a normally debited withdrawal:

```text
Original WITHDRAWAL debit
        ↓
CANCELLED
        ↓
CORRECTION credit
```

The original transaction is preserved.

---

# 10. Withdrawal Lifecycle

Statuses:

```text
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
```

Lifecycle:

```text
                 ┌──→ PROCESSING ──→ APPROVED
                 │         │
PENDING ─────────┤         └────────→ REJECTED
    │            │
    ├────────────┴────────────→ REJECTED
    │
    └─────────────────────────→ CANCELLED
```

The exact state guards implemented by `WithdrawalService` are authoritative.

---

# 11. Idempotency

Primary endpoint:

```http
POST /api/withdrawals
```

Header:

```http
Idempotency-Key: withdrawal-001
```

Flow:

```text
Request
   ↓
Validate key
   ↓
Normalize key
   ↓
Create request fingerprint
   ↓
Lookup user + key
       |
       +-- Not found → process
       |
       +-- Found → compare fingerprint
                       |
                       +-- Same → return existing
                       |
                       +-- Different → reject
```

### Same key + same request

The existing withdrawal is returned.

No second financial operation is created.

### Same key + different request

The request is rejected as an idempotency conflict.

### Concurrency

Idempotency works together with:

```text
Database uniqueness
Transactional boundaries
Wallet optimistic locking
Balance validation
Withdrawal state guards
```

---

# 12. Pagination

Supported pagination parameters:

```text
page
limit
```

Example:

```http
GET /api/wallet/transactions?page=1&limit=20
```

```http
GET /api/withdrawals?page=1&limit=20
```

The public page number is one-based.

Typical response fields:

```text
content
page
limit
totalElements
totalPages
hasNext
hasPrevious
```

---

# 13. Validation

Validation happens at multiple layers.

## Request validation

DTOs use Jakarta Bean Validation.

Examples:

```text
@NotNull
@NotBlank
@Email
@Positive
@DecimalMin
@Size
```

## Business validation

The service layer validates:

- User
- Account status/eligibility
- Currency
- Amount
- Wallet balance
- Payout method
- Payout option
- Method status
- Option status
- Method/option relationship
- Payout details
- Withdrawal ownership
- Withdrawal state
- Idempotency
- Fraud/risk

## Server-authoritative values

The backend determines:

```text
Wallet balance
Required VES
Payout value
Withdrawal amount
Withdrawal status
```

The client cannot override these values.

---

# 14. Payout Detail Validation

Payout details are validated according to the selected method.

Example:

```json
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "demo@veloop.test"
}
```

The backend resolves the method and validates the supplied details.

Invalid method-specific payout information is rejected before a normal withdrawal is created.

---

# 15. Fraud and Abuse Protection

Withdrawal creation passes through the fraud/risk layer.

Implemented rules:

```text
Rapid Withdrawal
Repeated Withdrawal
Repeated Failed Request
Unusual Wallet Activity
Suspicious Payout Request
Suspicious Account Activity
Multiple Suspicious Payout Pattern
```

Risk decisions:

```text
ALLOW
REVIEW
BLOCK
```

### BLOCK

```text
Risk evaluation
   ↓
BLOCK
   ↓
Persist fraud event
   ↓
Reject withdrawal
   ↓
No normal withdrawal
   ↓
No normal wallet debit
```

### REVIEW

```text
Risk evaluation
   ↓
REVIEW
   ↓
Create PENDING withdrawal
   ↓
Retain review information
   ↓
No normal initial debit
```

An authorized approval can perform the financial debit for a review withdrawal that has no linked transaction.

### ALLOW

```text
Risk evaluation
   ↓
ALLOW
   ↓
Resolve payout values
   ↓
Validate wallet balance
   ↓
Debit wallet
   ↓
Create withdrawal + ledger
```

---

# 16. Dynamic Risk Scoring

The risk engine uses configuration-driven scoring.

Conceptually:

```text
severity
   ↓
normalize to [0,1]
   ↓
nonlinear scoring
   ↓
severity² × maxRuleScore
   ↓
aggregate rule scores
   ↓
global decision
```

Global configuration includes:

```text
review threshold
block threshold
maximum score
```

Current default global thresholds:

```text
Review threshold = 30
Block threshold  = 60
Maximum score    = 100
```

Rule-specific configuration includes values such as:

```text
baseline
window
maxExpected
maxScore
```

Underlying historical activity is sourced from authoritative wallet, withdrawal and authentication data.

---

# 17. Rate Limiting

Redis-backed rate limiting protects sensitive APIs.

| Category | Limit | Window |
|---|---:|---:|
| Login | 5 requests | 60 seconds |
| Withdrawal creation | 5 requests | 60 seconds |
| Wallet mutation | 20 requests | 60 seconds |
| Withdrawal mutation | 20 requests | 60 seconds |

Configuration:

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
```

Exceeded limit:

```text
429 Too Many Requests
```

Blocked metric:

```text
rate_limit.blocked
```

The current implementation uses fail-open behavior for Redis/infrastructure failures.

---

# 18. Security

## JWT

```http
Authorization: Bearer <JWT>
```

## Passwords

Passwords are BCrypt-hashed.

## Authorization

Admin-only operations are protected using server-side authorization.

## Ownership

The authenticated principal determines user ownership.

## Financial protection

The backend owns:

```text
wallet balance
payout value
required VES
withdrawal status
```

## CORS

Allowed origins are configuration-driven.

## CSRF

CSRF is disabled because the API is stateless and JWT-based.

## Form Login / Basic Auth

These are not part of the API security model.

## Secrets

Never commit:

```text
.env
DB credentials
JWT secrets
API keys
production credentials
private keys
```

Use:

```text
.env.example
```

---

# 19. Error Handling

Common statuses:

| Status | Meaning |
|---:|---|
| 200 | Successful request |
| 201 | Resource created |
| 400 | Invalid request or business validation failure |
| 401 | Authentication required/failed |
| 403 | Authenticated but unauthorized |
| 404 | Resource not found |
| 409 | Conflict/idempotency/concurrency condition |
| 429 | Rate limit exceeded |
| 500 | Unexpected server error |

The application should not expose raw database exceptions or internal stack traces to clients.

---

# 20. Common Withdrawal Errors

Possible business errors include:

```text
User not found
Payout method not found
Selected payout method is inactive
Payout option not found
Selected payout option is inactive
Payout option does not belong to selected payout method
Insufficient VES balance
Withdrawal not found
Withdrawal ownership violation
Invalid withdrawal state
Invalid payout details
Invalid idempotency request
Withdrawal concurrency conflict
Fraud risk blocked
```

---

# 21. Wallet Financial Model

Supported currencies:

```text
VES
SVES
GEMS
TOKENS
SPINS
```

Credit:

```text
balance_after = balance_before + amount
```

Debit:

```text
balance_after = balance_before - amount
```

Every successful wallet mutation produces a ledger record.

Ledger data includes:

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

---

# 22. Withdrawal Financial Model

Normal withdrawal creation uses immediate VES deduction.

Example:

```text
Wallet:
25,000 VES

Withdrawal:
2,400 VES

After creation:
22,600 VES
```

If rejected or cancelled:

```text
22,600 VES
+
2,400 VES CORRECTION
=
25,000 VES
```

The original `WITHDRAWAL` ledger transaction is not deleted.

---

# 23. Auditability

Sensitive operations are audited.

Important events include:

```text
WALLET_CREDIT
WALLET_DEBIT

WITHDRAWAL_CREATED
WITHDRAWAL_PROCESSING
WITHDRAWAL_APPROVED
WITHDRAWAL_REJECTED
WITHDRAWAL_CANCELLED

RECONCILIATION_FAILURE
```

Withdrawal-specific lifecycle records are also stored.

Fraud decisions are stored in `fraud_risk_events`.

---

# 24. Reconciliation

Admin endpoint:

```http
GET /api/admin/reconciliation/wallet/{walletId}?currency=VES
```

### Authentication

Required.

### Authorization

Admin.

### Comparison

```text
Stored Wallet Balance
        vs
Ledger-Derived Balance
```

### Success

```text
Stored balance = ledger-derived balance
        ↓
reconciliation.success
```

### Failure

```text
Stored balance != ledger-derived balance
        ↓
reconciliation.failure
        ↓
Audit record
```

Reconciliation detects inconsistencies. It does not silently modify balances.

---

# 25. Observability

## Request headers

```http
X-Correlation-ID
X-Request-ID
```

These identifiers support request tracing.

## Request completion logging

Structured request information includes:

```text
event=request.completed
method
path
status
durationMs
correlationId
requestId
```

## Application metrics

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

application.error
```

---

# 26. Actuator APIs

Health:

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

Metrics:

```http
GET /actuator/metrics
```

OpenAPI:

```http
GET /v3/api-docs
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

---

# 27. Complete API Endpoint Summary

| Method | Endpoint | Auth | Authorization |
|---|---|---|---|
| POST | `/api/auth/register` | No | Public |
| POST | `/api/auth/login` | No | Public |
| GET | `/api/auth/me` | JWT | Authenticated |
| GET | `/api/wallet` | JWT | User |
| GET | `/api/wallet/transactions` | JWT | User |
| GET | `/api/wallet/summary` | JWT | User |
| POST | `/api/wallet/credit` | JWT | ADMIN |
| POST | `/api/wallet/debit` | JWT | ADMIN |
| GET | `/api/payouts/configuration` | JWT | Authenticated |
| POST | `/api/withdrawals` | JWT | User |
| GET | `/api/withdrawals` | JWT | User |
| GET | `/api/withdrawals/{withdrawalId}` | JWT | Owner |
| PATCH | `/api/withdrawals/{withdrawalId}/processing` | JWT | ADMIN |
| PATCH | `/api/withdrawals/{withdrawalId}/approve` | JWT | ADMIN |
| PATCH | `/api/withdrawals/{withdrawalId}/reject` | JWT | ADMIN |
| PATCH | `/api/withdrawals/{withdrawalId}/cancel` | JWT | USER + Owner |
| GET | `/api/admin/reconciliation/wallet/{walletId}` | JWT | ADMIN |
| GET | `/actuator/health` | No | Operational |
| GET | `/actuator/health/liveness` | No | Operational |
| GET | `/actuator/health/readiness` | No | Operational |
| GET | `/actuator/metrics` | No/Configured | Operational |
| GET | `/v3/api-docs` | No/Configured | API documentation |
| GET | `/swagger-ui.html` | No/Configured | API documentation |

---

# 28. End-to-End Withdrawal Example

## Step 1 — Login

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "demo@veloop.test",
  "password": "Demo@12345"
}
```

Receive JWT.

## Step 2 — Get payout configuration

```http
GET /api/payouts/configuration
Authorization: Bearer <JWT>
```

Select:

```text
payoutMethodId
payoutOptionId
```

## Step 3 — Create withdrawal

```http
POST /api/withdrawals
Authorization: Bearer <JWT>
Idempotency-Key: withdrawal-001
Content-Type: application/json
```

```json
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "demo@veloop.test"
}
```

## Step 4 — Validation

```text
Authentication
      ↓
Authorization
      ↓
Idempotency
      ↓
Eligibility
      ↓
Payout method
      ↓
Payout option
      ↓
Payout details
      ↓
Fraud/risk
      ↓
Wallet balance
```

## Step 5 — Financial operation

Normal ALLOW path:

```text
Resolve required VES
      ↓
Wallet debit
      ↓
Ledger transaction
      ↓
Withdrawal
      ↓
Audit
      ↓
PENDING
```

## Step 6 — Processing

```http
PATCH /api/withdrawals/{withdrawalId}/processing
Authorization: Bearer <ADMIN-JWT>
```

```text
PENDING → PROCESSING
```

## Step 7 — Approve or reject

Approve:

```http
PATCH /api/withdrawals/{withdrawalId}/approve
```

Reject:

```http
PATCH /api/withdrawals/{withdrawalId}/reject?rejectionReason=Invalid%20request
```

## Step 8 — Cancellation

For eligible pending withdrawal:

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
Authorization: Bearer <USER-JWT>
```

```text
PENDING
   ↓
CANCELLED
   ↓
CORRECTION ledger transaction
```

---

# 29. API Design Principles

1. Backend is the financial source of truth.
2. JWT identity determines the user.
3. Authorization is enforced server-side.
4. Resource ownership is enforced server-side.
5. Payout configuration comes from the backend.
6. Required VES is resolved by the backend.
7. Successful wallet mutations create ledger records.
8. Withdrawal creation uses immediate deduction for the normal ALLOW path.
9. Rejection/cancellation uses compensating ledger transactions.
10. Withdrawal creation uses idempotency.
11. Sensitive APIs use rate limiting.
12. Fraud/risk controls participate in withdrawal creation.
13. Sensitive operations are audited.
14. Reconciliation verifies wallet and ledger consistency.
15. Financial history is not deleted to reverse operations.
16. Concurrent financial updates are protected.

---

# 30. Source-of-Truth Rule

This document describes the implemented API behavior.

When source code changes an API contract, this documentation must be updated in the same change.

Do not document:

- planned endpoints as implemented
- removed endpoints
- frontend-only financial logic
- client-authoritative wallet balances
- client-authoritative withdrawal amounts
- unsupported withdrawal state transitions
- unsupported provider behavior
- authentication rules that are not enforced by the backend

The implementation remains the authoritative source for API behavior.

---

# 31. Verification Status

Current backend verification:

```text
Automated regression:
257 / 257 PASS

Failures:
0

Errors:
0

Manual API verification:
COMPLETED

Demo/seed data:
VERIFIED

Postman collection:
COMPLETED

Architecture documentation:
COMPLETED

Database documentation:
COMPLETED

Security documentation:
COMPLETED

Testing documentation:
COMPLETED

Reconciliation documentation:
COMPLETED
```

Backend status:

```text
READY FOR FRONTEND / DEPLOYMENT / DEMONSTRATION
```