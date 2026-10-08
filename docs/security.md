# VELoop Rewards Backend — Security

## 1. Security Goals

Protect:

- authentication
- authorization
- resource ownership
- wallet balances
- payout values
- withdrawal operations
- credentials and secrets
- request volume
- duplicate financial requests
- financial auditability

## 2. Authentication

JWT Bearer authentication is used for protected APIs.

```http
Authorization: Bearer <JWT>
```

Flow:

```text
Login
  ↓
JWT issued
  ↓
JwtAuthenticationFilter
  ↓
JWT validation
  ↓
SecurityContext
  ↓
Controller / service
```

JWT expiration defaults to 900000 ms (15 minutes) unless configured differently.

## 3. Registration and Passwords

Registration validates name, email and password.

Password constraints include 8 to 100 characters in the current request DTO.

Passwords are encoded using BCrypt and are not stored or returned in plaintext.

## 4. Authorization

Administrative endpoints use method-level role authorization.

Examples:

```text
POST /api/wallet/credit                  -> ADMIN
POST /api/wallet/debit                   -> ADMIN
PATCH /api/withdrawals/{id}/processing   -> ADMIN
PATCH /api/withdrawals/{id}/approve      -> ADMIN
PATCH /api/withdrawals/{id}/reject       -> ADMIN
PATCH /api/withdrawals/{id}/cancel       -> USER role + owner
GET   /api/admin/reconciliation/...      -> ADMIN
```

## 5. Ownership

The authenticated principal, not a client-supplied user ID, identifies the wallet/withdrawal owner for user operations.

This prevents simple cross-user access by changing an ID in a request body, query parameter or frontend state.

## 6. Financial Security

The backend controls:

```text
wallet balance
required VES
payout amount
withdrawal amount
withdrawal status
payout configuration
```

The withdrawal request contains only method ID, option ID and payout details. Financial values are resolved from backend configuration.

## 7. Input Validation

Validation covers:

- required fields
- email format
- password length
- positive payout method/option identifiers
- payout details length
- currency and amount validity
- payout method state
- payout option state
- method/option relationship
- wallet balance
- withdrawal ownership/state
- idempotency

## 8. CORS

Allowed origins are configuration-driven using `CORS_ALLOWED_ORIGINS`.

Production deployments should use explicit trusted origins rather than permissive configuration.

## 9. CSRF and Session Model

The API is designed as a stateless JWT service rather than a browser session application.

Therefore CSRF protection is disabled and form login/HTTP Basic are not part of the API authentication model.

## 10. Secrets and Configuration

Sensitive configuration is externalized.

Do not commit:

```text
.env
real DB credentials
JWT secrets
API keys
production credentials
```

`.env.example` documents expected configuration without real secrets.

## 11. Rate Limiting

Redis-backed rate limiting protects sensitive operations:

```text
Login:                5 / 60 seconds
Withdrawal creation:  5 / 60 seconds
Wallet mutations:    20 / 60 seconds
Withdrawal mutations:20 / 60 seconds
```

A configured limit violation returns `429 Too Many Requests`.

Infrastructure/Redis failures use fail-open behavior in the current implementation so Redis outage does not incorrectly deny all traffic.

## 12. Idempotency

Withdrawal creation requires a valid idempotency key at the service layer.

The key is scoped to the authenticated user and combined with a request fingerprint.

The database uniqueness constraint adds another protection layer.

## 13. Fraud and Abuse Protection

Withdrawal creation evaluates seven risk rules:

```text
Rapid Withdrawal
Repeated Withdrawal
Repeated Failed Request
Unusual Wallet Activity
Suspicious Payout Request
Suspicious Account Activity
Multiple Suspicious Payout Pattern
```

Each rule calculates normalized severity and a nonlinear score contribution.

Global decisions are:

```text
ALLOW
REVIEW
BLOCK
```

The risk score is capped by the configured maximum score.

Current default global thresholds are:

```text
review threshold = 30
block threshold  = 60
maximum score    = 100
```

Rule window/baseline/max-expected/max-score values are configuration-driven.

The authoritative activity queries use wallet/withdrawal/authentication data rather than treating the fraud-event table as the source of the underlying financial history.

## 14. Concurrency Security

Wallet updates use optimistic locking through the wallet `version` field.

Payout-job processing uses pessimistic locking when loading a job.

Database uniqueness constraints prevent duplicate logical records in critical areas.

## 15. Auditability

Sensitive wallet and withdrawal operations create audit records.

Important actions include:

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

## 16. Error Security

Expected failures are mapped to controlled API errors.

Clients should not receive raw database exceptions, passwords, JWT secrets or internal stack traces as normal API responses.

## 17. Security Principles

1. Authenticate protected requests.
2. Authorize privileged operations.
3. Enforce resource ownership.
4. Hash passwords.
5. Externalize secrets.
6. Validate requests server-side.
7. Rate-limit sensitive operations.
8. Use idempotency for retryable financial operations.
9. Protect concurrent financial updates.
10. Audit sensitive changes.
11. Treat backend data as the financial source of truth.