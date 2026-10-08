# VELoop Rewards Backend API Documentation

## 1. Overview

VELoop Rewards Backend is a Spring Boot REST API for a backend-controlled rewards wallet, payout configuration, withdrawal processing, security, audit logging, fraud protection, reconciliation, and observability.

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

Administrative operations require the appropriate administrative authorization.

Admin operations include:

- Wallet credit
- Wallet debit
- Withdrawal processing
- Withdrawal approval
- Withdrawal rejection
- Administrative withdrawal management

Users cannot perform administrative wallet mutations.

Users also cannot access another user's wallet or withdrawal records.

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

The registration request contains the user registration information.

Example:

```json
{
  "name": "Demo User",
  "email": "demo@example.com",
  "password": "Password@123"
}
```

### Validation

The registration request validates fields such as:

- Name
- Email
- Password

Email must be valid.

Password validation is performed before account creation.

Duplicate email addresses are rejected.

Passwords are stored using password hashing and are never stored as plaintext.

### Success

```text
201 Created
```

The successful response follows the application's standard API response structure.

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

### Validation

The backend validates:

- Email
- Password
- User existence
- Account status
- Password correctness

### Success

```text
200 OK
```

The response contains the authenticated user information and JWT access token.

Example structure:

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

Returns information for the currently authenticated user.

The user identity is obtained from the authenticated security context.

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

### Response

The response contains the authenticated user's wallet balances.

The wallet supports:

```text
VES
SVES
GEMS
TOKENS
SPINS
```

Conceptual response:

```json
{
  "success": true,
  "message": "Wallet retrieved successfully",
  "data": {
    "ves": 10000.0000,
    "sves": 500.0000,
    "gems": 100.0000,
    "tokens": 50.0000,
    "spins": 5.0000
  }
}
```

The exact numeric values are determined by the database.

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

The endpoint supports pagination.

Example:

```http
GET /api/wallet/transactions?page=1&limit=20
```

The API returns only transactions belonging to the authenticated user.

### Pagination response

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

### Transaction information

Wallet ledger records contain information including:

- Transaction ID
- User ID
- Wallet ID
- Currency
- Transaction type
- Amount
- Balance before
- Balance after
- Source
- Reference ID
- Status
- Description
- Metadata
- Created timestamp

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

The wallet summary contains:

```text
VES
SVES
GEMS
TOKENS
SPINS
totalTransactions
```

Conceptual response:

```json
{
  "success": true,
  "message": "Wallet summary retrieved successfully",
  "data": {
    "ves": 10000.0000,
    "sves": 500.0000,
    "gems": 100.0000,
    "tokens": 50.0000,
    "spins": 5.0000,
    "totalTransactions": 12
  }
}
```

---

# 7. Administrative Wallet APIs

Wallet mutations are administrative/internal operations.

Normal users cannot directly call these operations.

## 7.1 Credit Wallet

```http
POST /api/wallet/credit
```

### Authentication

Required.

### Authorization

Admin/internal authorization required.

### Request

Conceptually:

```json
{
  "userId": 10,
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

The backend:

1. Validates the request.
2. Loads the wallet.
3. Performs the wallet mutation.
4. Creates the corresponding ledger transaction.
5. Records the sensitive operation through the audit mechanism.

The client cannot directly set the final wallet balance.

---

## 7.2 Debit Wallet

```http
POST /api/wallet/debit
```

### Authentication

Required.

### Authorization

Admin/internal authorization required.

### Request

Conceptually:

```json
{
  "userId": 10,
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

- User
- Currency
- Amount
- Wallet availability
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

### Request

No request body.

### Response

The API returns backend-controlled active payout configuration.

It contains:

- Active payout methods
- Active payout options
- Payout amount
- Currency
- Required currency amount

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

Inactive payout methods and inactive payout options are excluded.

The frontend must not hardcode payout values.

---

# 9. Withdrawal APIs

## 9.1 Create Withdrawal

```http
POST /api/withdrawals
```

### Authentication

Required.

### Authorization

Authenticated user.

The withdrawal is created for the authenticated user.

### Required Header

```http
Idempotency-Key: <unique-key>
```

### Request

```json
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "user@upi"
}
```

### Important

The request does **not** contain the final VES amount to deduct.

The backend resolves the financial values from the selected payout option.

The backend determines:

```text
Payout Method
        ↓
Payout Option
        ↓
Payout Amount
        ↓
Required VES
        ↓
Wallet Debit
```

The client cannot manipulate the required VES value.

### Validation

The backend validates:

- Idempotency key
- Request fingerprint
- User existence
- User withdrawal eligibility
- Payout method existence
- Payout method validity
- Payout method active status
- Payout option existence
- Payout option validity
- Payout option active status
- Payout option/method relationship
- Method-specific payout details
- Required VES balance
- Fraud and abuse risk controls

### Financial behavior

The implemented strategy is:

```text
Validate withdrawal
        ↓
Resolve payout configuration
        ↓
Validate wallet balance
        ↓
Debit required VES
        ↓
Create WITHDRAWAL ledger transaction
        ↓
Create PENDING withdrawal
        ↓
Create audit records
```

The wallet deduction occurs when the withdrawal is created.

---

## 9.2 Get Withdrawal History

```http
GET /api/withdrawals?page=1&limit=20
```

### Authentication

Required.

### Authorization

Authenticated user.

### Ownership

Only withdrawals belonging to the authenticated user are returned.

### Pagination

The service returns:

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

Withdrawals are returned using the repository's created-date descending order.

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

The requested withdrawal must belong to the authenticated user.

Attempting to access another user's withdrawal is rejected.

### Response

A withdrawal response represents information including:

```text
withdrawalId
user
payout method
payout option
currency
currency amount
payout amount
payout details
status
rejection reason
review note
transaction
requestedAt
processedAt
createdAt
updatedAt
```

The exact serialized response follows the current `WithdrawalResponse` DTO.

---

## 9.4 Process Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/processing
```

### Authentication

Required.

### Authorization

Admin authorization required.

### Allowed transition

```text
PENDING → PROCESSING
```

A withdrawal that is not `PENDING` cannot be moved into `PROCESSING`.

---

## 9.5 Approve Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/approve
```

### Authentication

Required.

### Authorization

Admin authorization required.

### Allowed transition

Approval is allowed according to the implemented withdrawal state rules.

The approval operation does **not** perform another wallet deduction.

The original withdrawal deduction remains represented by the original ledger transaction.

---

## 9.6 Reject Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/reject
```

### Authentication

Required.

### Authorization

Admin authorization required.

### Rejection reason

A rejection requires a rejection reason.

### Financial behavior

A rejected withdrawal reverses the original wallet deduction.

Conceptually:

```text
Withdrawal created
       ↓
VES deducted
       ↓
REJECTED
       ↓
Compensating VES credit
       ↓
CORRECTION ledger transaction
```

The original transaction is not deleted.

---

## 9.7 Cancel Withdrawal

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
```

### Authentication

Required.

### Authorization

Authenticated owner.

### Allowed transition

```text
PENDING → CANCELLED
```

Cancellation reverses the original wallet deduction.

A compensating ledger transaction is created.

Users cannot use cancellation to arbitrarily modify a withdrawal that has already moved beyond the allowed state.

---

# 10. Withdrawal Lifecycle

The implemented withdrawal statuses are:

```text
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
```

Lifecycle:

```text
                    ┌── APPROVED
                    │
PENDING ──→ PROCESSING
    │               │
    │               └── REJECTED
    │
    ├── REJECTED
    │
    └── CANCELLED
```

## Creation

```text
POST /api/withdrawals
        ↓
PENDING
```

The wallet is debited immediately.

## Processing

```text
PENDING
   ↓
PROCESSING
```

Admin operation.

## Approval

```text
PROCESSING
   ↓
APPROVED
```

Approval does not perform another wallet debit.

## Rejection

```text
PENDING/PROCESSING
        ↓
    REJECTED
        ↓
Wallet reversal
```

## Cancellation

```text
PENDING
   ↓
CANCELLED
   ↓
Wallet reversal
```

Terminal withdrawal states must not be arbitrarily changed into unrelated states.

---

# 11. Idempotency

Withdrawal creation uses an idempotency key.

Header:

```http
Idempotency-Key: withdrawal-001
```

The backend:

```text
Idempotency-Key
       ↓
Normalize key
       ↓
Create request fingerprint
       ↓
Find existing request
       ↓
Compare fingerprint
       ↓
Return existing withdrawal
```

## Same key + same request

A repeated request using the same key and equivalent request fingerprint returns the existing withdrawal rather than creating another withdrawal.

This prevents:

- Double clicks
- Client retries
- Network retries
- Duplicate submissions
- Multiple wallet deductions for one logical request

## Same key + different request

The idempotency fingerprint is checked.

A previously used idempotency key must not be reused for a different withdrawal request.

## Concurrent requests

The idempotency layer also participates in withdrawal concurrency protection.

The objective is:

```text
Two identical requests
        ↓
One logical withdrawal
        ↓
One wallet deduction
        ↓
One withdrawal record
```

---

# 12. Pagination

The APIs use Spring Data pagination internally and expose a normalized page response.

Pagination parameters are documented as:

```text
page
limit
```

Example:

```http
GET /api/withdrawals?page=1&limit=20
```

Example:

```http
GET /api/wallet/transactions?page=1&limit=20
```

The response contains:

```text
content
page
limit
totalElements
totalPages
hasNext
hasPrevious
```

The public page number is one-based.

---

# 13. Validation

Validation occurs at multiple layers.

## Request validation

Request DTOs use Jakarta Bean Validation where applicable.

Examples include:

```text
@NotBlank
@NotNull
@Email
@Size
```

## Business validation

The service layer validates:

- User existence
- Account eligibility
- Currency
- Amount
- Wallet balance
- Payout method
- Payout option
- Payout method status
- Payout option status
- Payout method/option relationship
- Payout details
- Withdrawal ownership
- Withdrawal state
- Idempotency
- Fraud/risk rules

## Server-authoritative financial values

The client cannot determine:

```text
Current wallet balance
Required VES
Payout value
Withdrawal amount
Withdrawal status
```

These values are resolved and validated by the backend.

---

# 14. Payout Detail Validation

Withdrawal payout details are validated according to the selected payout method.

The backend receives:

```json
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "user@upi"
}
```

The service resolves the payout method and passes the method code and supplied details to the payout detail validator.

Invalid method-specific payout details are rejected before the withdrawal is created.

---

# 15. Fraud and Abuse Protection

Withdrawal creation also passes through the fraud/risk evaluation layer.

The system can evaluate withdrawal activity using the configured fraud rules.

A high-risk request can be blocked.

A request requiring review can be retained with a review indication rather than being silently treated as a normal withdrawal.

The final financial withdrawal operation therefore follows:

```text
Authentication
      ↓
Authorization
      ↓
Request validation
      ↓
Idempotency
      ↓
Eligibility
      ↓
Payout validation
      ↓
Fraud/risk evaluation
      ↓
Wallet validation
      ↓
Wallet debit
      ↓
Withdrawal creation
```

---

# 16. Rate Limits

Rate limiting is enabled through the configured security rate-limit system.

Current configuration:

| API category | Limit | Window |
|---|---:|---:|
| Login | 5 requests | 60 seconds |
| Withdrawal creation | 5 requests | 60 seconds |
| Wallet mutations | 20 requests | 60 seconds |
| Withdrawal mutations | 20 requests | 60 seconds |

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

When a configured rate limit is exceeded:

```text
429 Too Many Requests
```

The application also records the observability metric:

```text
rate_limit.blocked
```

The rate limiter is designed to fail open for infrastructure/Redis failures rather than incorrectly blocking all requests because the rate-limit backend is unavailable.

---

# 17. Error Handling

The application uses centralized exception handling.

Expected errors are translated into appropriate HTTP responses.

## Common HTTP statuses

| Status | Meaning |
|---:|---|
| 200 | Successful request |
| 201 | Resource created |
| 400 | Invalid request or validation/business error |
| 401 | Authentication required/failed |
| 403 | Authenticated but not authorized |
| 404 | Resource not found |
| 409 | Conflict/idempotency/concurrency condition |
| 429 | Rate limit exceeded |
| 500 | Unexpected application error |

The exact error body depends on the exception handled by the application's global exception handling layer.

Clients should not receive raw database exceptions or internal stack traces as normal API responses.

---

# 18. Common Withdrawal Errors

Examples of business errors include:

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

# 19. Security Errors

## Missing or invalid JWT

```text
401 Unauthorized
```

Example:

```http
Authorization: Bearer <invalid-token>
```

## Insufficient authorization

```text
403 Forbidden
```

Example:

```text
USER attempting ADMIN-only wallet mutation
```

## Ownership violation

A user cannot retrieve or mutate another user's withdrawal.

---

# 20. Wallet Financial Model

Wallet balances are backend-controlled.

Supported currencies:

```text
VES
SVES
GEMS
TOKENS
SPINS
```

Every successful wallet mutation creates a corresponding ledger transaction.

Conceptually:

```text
Credit:

balanceAfter = balanceBefore + amount
```

```text
Debit:

balanceAfter = balanceBefore - amount
```

The ledger preserves:

```text
balanceBefore
balanceAfter
amount
currency
transactionType
source
referenceId
status
```

---

# 21. Withdrawal Financial Model

The current strategy is immediate deduction.

Example:

```text
Wallet balance
10,000 VES

Withdrawal requires
2,400 VES

After creation
7,600 VES
```

The ledger records the withdrawal debit.

If the withdrawal is rejected:

```text
7,600 VES
    +
2,400 VES reversal
    =
10,000 VES
```

If the withdrawal is cancelled while eligible:

```text
7,600 VES
    +
2,400 VES reversal
    =
10,000 VES
```

The original debit remains in the ledger and the reversal is represented separately.

---

# 22. Auditability

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

Withdrawal lifecycle changes retain the actor and state transition information needed for investigation.

---

# 23. Reconciliation

The reconciliation API compares the stored wallet balance with the balance derived from completed ledger transactions.

Conceptually:

```text
Stored Wallet Balance
        vs
Ledger-Derived Balance
```

If the values match:

```text
reconciliation.success
```

is recorded.

If they differ:

```text
reconciliation.failure
```

is recorded and the reconciliation failure is audited.

Reconciliation is designed to detect accounting inconsistencies rather than silently correcting them.

---

# 24. Observability Headers

The API supports request and correlation identifiers.

Headers:

```http
X-Correlation-ID
X-Request-ID
```

If a correlation ID is supplied and valid, it is propagated.

Request IDs are generated by the request filter.

The IDs are also placed into the logging context so that a request can be traced through structured logs.

The response includes the identifiers.

---

# 25. Structured Logging

HTTP request completion is logged using structured logging information including:

```text
event=request.completed
method
path
status
durationMs
correlationId
requestId
```

This allows individual API requests to be correlated with application logs.

---

# 26. Actuator Endpoints

Health endpoints are exposed for application health monitoring.

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

Metrics are exposed through the Actuator metrics endpoint according to the application's management endpoint configuration.

Examples of application metrics include:

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

# 27. API Endpoint Summary

| Method | Endpoint | Authentication | Authorization |
|---|---|---|---|
| POST | `/api/auth/register` | No | Public |
| POST | `/api/auth/login` | No | Public |
| GET | `/api/auth/me` | JWT | Authenticated |
| GET | `/api/wallet` | JWT | User |
| GET | `/api/wallet/transactions` | JWT | User |
| GET | `/api/wallet/summary` | JWT | User |
| POST | `/api/wallet/credit` | JWT | Admin/Internal |
| POST | `/api/wallet/debit` | JWT | Admin/Internal |
| GET | `/api/payouts/configuration` | JWT | Authenticated |
| POST | `/api/withdrawals` | JWT | User |
| GET | `/api/withdrawals` | JWT | User |
| GET | `/api/withdrawals/{withdrawalId}` | JWT | Owner |
| PATCH | `/api/withdrawals/{withdrawalId}/processing` | JWT | Admin |
| PATCH | `/api/withdrawals/{withdrawalId}/approve` | JWT | Admin |
| PATCH | `/api/withdrawals/{withdrawalId}/reject` | JWT | Admin |
| PATCH | `/api/withdrawals/{withdrawalId}/cancel` | JWT | Owner |
| GET | `/actuator/health` | Public | Health endpoint |
| GET | `/actuator/health/liveness` | Public | Health endpoint |
| GET | `/actuator/health/readiness` | Public | Health endpoint |

---

# 28. Example End-to-End Withdrawal Flow

## Step 1 — Authenticate

```http
POST /api/auth/login
```

Receive JWT.

## Step 2 — Get payout configuration

```http
GET /api/payouts/configuration
Authorization: Bearer <JWT>
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
  "payoutDetails": "user@upi"
}
```

## Step 4 — Backend validates

```text
Authentication
      ↓
Authorization
      ↓
Idempotency
      ↓
User eligibility
      ↓
Payout method
      ↓
Payout option
      ↓
Payout details
      ↓
Fraud/risk controls
      ↓
Wallet balance
```

## Step 5 — Financial operation

```text
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

## Step 6 — Admin processing

```http
PATCH /api/withdrawals/{withdrawalId}/processing
```

```text
PENDING → PROCESSING
```

## Step 7 — Admin decision

Approve:

```http
PATCH /api/withdrawals/{withdrawalId}/approve
```

or reject:

```http
PATCH /api/withdrawals/{withdrawalId}/reject
```

If rejected:

```text
REJECTED
   ↓
VES reversal
   ↓
CORRECTION ledger transaction
```

## Step 8 — User cancellation

If still eligible:

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
```

```text
PENDING → CANCELLED
        ↓
VES reversal
```

---

# 29. API Design Principles

The API follows these principles:

1. The backend is the financial source of truth.
2. JWT authentication determines user identity.
3. Authorization is enforced server-side.
4. Users cannot access another user's wallet or withdrawal data.
5. Payout configuration is backend-controlled.
6. Required VES is resolved from the backend.
7. Wallet mutations create ledger records.
8. Withdrawal creation deducts VES immediately.
9. Rejection and cancellation use compensating ledger transactions.
10. Withdrawal creation is idempotent.
11. Sensitive endpoints are rate limited.
12. Fraud/risk controls participate in withdrawal creation.
13. Sensitive operations are audited.
14. Request and correlation IDs support tracing.
15. Pagination prevents unbounded transaction/withdrawal responses.

---

# 30. Source-of-Truth Rule

This document describes the implemented API behavior.

When API behavior changes in source code, this document must be updated in the same change.

Do not document:

- Planned endpoints as implemented endpoints
- Removed endpoints
- Frontend-only behavior
- Hardcoded payout values that are not in backend configuration
- Unsupported withdrawal transitions
- Authentication behavior that is not enforced by Spring Security

The implementation remains the authoritative source for API behavior.

---
