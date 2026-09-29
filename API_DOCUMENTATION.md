# VELoop Rewards — API Documentation

**Project:** Wallet & Withdrawal Backend
**Checkpoint:** `V3-WITHDRAWAL-SYSTEM-COMPLETE` (Phase 6 complete and verified)
**Next Phase:** Phase 7 — Idempotency + Withdrawal Concurrency

---

## 1. Overview

REST API for wallet management, payout configuration, and withdrawals.

**Tech stack:** Java 21 · Spring Boot 3.5.16 · Spring Security (JWT) · Spring Data JPA / Hibernate · MySQL 8.x · Flyway · Jakarta Bean Validation · Springdoc OpenAPI

**The backend is the single source of truth for:** wallet balances and transactions, reward currencies, payout configuration, withdrawal amounts / eligibility / status, wallet deductions and reversals, and authorization and ownership.

> The frontend must never be trusted with financial values.

**Base URL (local):** `http://localhost:8080` (host and port may vary by environment)
**Swagger UI (local):** `http://localhost:8080/swagger-ui/index.html`

---

## 2. Authentication

Protected endpoints require a JWT Bearer token:

```
Authorization: Bearer <JWT_TOKEN>
```

- The user is always resolved from the JWT.
- Clients must **not** send a `userId` to identify a wallet or withdrawal owner.

## 3. Response Format

Responses use the project's standard wrapper (exact fields follow the implemented DTOs):

```json
{
  "success": true,
  "message": "Request successful",
  "data": {}
}
```

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Resource created |
| 400 | Validation or business rule failure |
| 401 | Missing or invalid authentication |
| 403 | Not authorized (role or ownership) |
| 404 | Resource not found |
| 409 | Resource/state conflict (where applicable) |
| 500 | Unexpected server error |

---

## 4. Endpoint Summary

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| GET | `/api/wallet` | User | Get current wallet |
| GET | `/api/wallet/transactions` | User | Transaction history (paginated) |
| GET | `/api/wallet/summary` | User | Wallet summary |
| POST | `/api/wallet/credit` | Admin | Credit a wallet |
| POST | `/api/wallet/debit` | Admin | Debit a wallet |
| GET | `/api/payouts/configuration` | User | Active payout methods and options |
| POST | `/api/withdrawals` | User | Create withdrawal |
| GET | `/api/withdrawals` | User | List own withdrawals (paginated) |
| GET | `/api/withdrawals/{withdrawalId}` | Owner | Withdrawal details |
| PATCH | `/api/withdrawals/{withdrawalId}/processing` | Admin | `PENDING → PROCESSING` |
| PATCH | `/api/withdrawals/{withdrawalId}/approve` | Admin | Approve withdrawal |
| PATCH | `/api/withdrawals/{withdrawalId}/reject` | Admin | Reject and reverse deduction |
| PATCH | `/api/withdrawals/{withdrawalId}/cancel` | Owner | Cancel pending withdrawal |

---

## 5. Wallet APIs

### 5.1 Get Current Wallet — `GET /api/wallet`

Returns the authenticated user's wallet. No request body.

```json
{
  "success": true,
  "message": "Wallet retrieved successfully",
  "data": {
    "id": 1,
    "userId": 10,
    "ves": 10000,
    "sves": 500,
    "gems": 250,
    "tokens": 100,
    "spins": 5,
    "withdrawnVes": 0,
    "createdAt": "2026-01-01T10:00:00",
    "updatedAt": "2026-01-10T10:00:00"
  }
}
```

**Errors:** `401` invalid/missing JWT · `404` wallet not found

### 5.2 Get Wallet Transactions — `GET /api/wallet/transactions`

**Query params (optional):** `page`, `limit` — e.g. `?page=1&limit=20`

```json
{
  "success": true,
  "message": "Wallet transactions retrieved successfully",
  "data": {
    "content": [
      {
        "transactionId": "TXN-10002",
        "userId": 10,
        "walletId": 1,
        "currency": "VES",
        "transactionType": "WITHDRAWAL",
        "amount": -2400,
        "balanceBefore": 6000,
        "balanceAfter": 3600,
        "source": "WITHDRAWAL",
        "referenceId": "WD-10001",
        "status": "COMPLETED",
        "description": "Wallet withdrawal",
        "createdAt": "2026-01-10T11:00:00"
      }
    ],
    "page": 1,
    "limit": 20,
    "totalElements": 1
  }
}
```

**Errors:** `401`

### 5.3 Get Wallet Summary — `GET /api/wallet/summary`

```json
{
  "success": true,
  "message": "Wallet summary retrieved successfully",
  "data": {
    "ves": 10000,
    "sves": 500,
    "gems": 250,
    "tokens": 100,
    "spins": 5,
    "withdrawnVes": 2400
  }
}
```

**Errors:** `401`

### 5.4 Credit Wallet — `POST /api/wallet/credit` *(ROLE_ADMIN)*

Credits a wallet currency and writes a ledger transaction. Normal users cannot credit their own wallet.

```json
{
  "userId": 10,
  "currency": "VES",
  "amount": 1000,
  "transactionType": "ADMIN_CREDIT",
  "source": "ADMIN",
  "description": "Administrative wallet credit"
}
```

**Response `data`:** `currency`, `amount`, `balanceBefore`, `balanceAfter`
**Errors:** `400` invalid amount/currency/data · `401` · `403` not admin

### 5.5 Debit Wallet — `POST /api/wallet/debit` *(ROLE_ADMIN)*

Debits a wallet after validating available balance, and writes a ledger transaction. Request and response mirror credit (`transactionType: "ADMIN_DEBIT"`).

**Errors:** `400` invalid amount, invalid currency, insufficient balance, or validation failure · `401` · `403`

---

## 6. Payout Configuration

### `GET /api/payouts/configuration`

Returns active payout methods and options only. Inactive methods/options are excluded.

```json
{
  "success": true,
  "message": "Payout configuration retrieved successfully",
  "data": [
    {
      "id": 1,
      "code": "UPI",
      "name": "UPI",
      "active": true,
      "options": [
        { "id": 1, "payoutAmount": 10, "currency": "INR", "currencyAmount": 2400, "active": true },
        { "id": 2, "payoutAmount": 25, "currency": "INR", "currencyAmount": 5800, "active": true }
      ]
    }
  ]
}
```

**Field mapping:**

| Field | Meaning |
|-------|---------|
| `payoutAmount` | Actual payout value |
| `currency` | Payout currency |
| `currencyAmount` | **Required VES** |

Example: `payoutAmount = 10`, `currency = INR`, `currencyAmount = 2400` → ₹10 requires 2,400 VES.
The frontend must not calculate or submit the required VES.

**Errors:** `401`

---

## 7. Withdrawal APIs

### 7.1 Create Withdrawal — `POST /api/withdrawals`

Creates a withdrawal for the authenticated user. The backend resolves the payout method, option, amount, required VES, currency, and active status. **The client never supplies an amount.**

**Request:**

```json
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "test@upi"
}
```

> **Security:** Do not send an `amount` field. Required VES is always derived from the selected payout option.

**Processing flow:**

1. Validate JWT user
2. Validate payout method (exists, active)
3. Validate payout option (exists, active)
4. Validate option belongs to method
5. Resolve required VES
6. Validate wallet balance
7. Debit VES
8. Create `WITHDRAWAL` ledger transaction
9. Create `PENDING` withdrawal

**Response:**

```json
{
  "success": true,
  "message": "Withdrawal created successfully",
  "data": {
    "withdrawalId": "WD-10001",
    "userId": 10,
    "payoutMethodId": 1,
    "payoutOptionId": 1,
    "currency": "INR",
    "currencyAmount": 2400,
    "payoutAmount": 10,
    "payoutDetails": "test@upi",
    "status": "PENDING",
    "requestedAt": "2026-01-10T12:00:00"
  }
}
```

**Errors:** `401` · `400` for any of: invalid or inactive method, invalid or inactive option, option/method mismatch, insufficient VES, invalid payout details, validation failure.

### 7.2 List Withdrawals — `GET /api/withdrawals`

Returns only the authenticated user's withdrawals. **Query params (optional):** `page`, `limit`.

```json
{
  "success": true,
  "message": "Withdrawals retrieved successfully",
  "data": {
    "content": [
      {
        "withdrawalId": "WD-10001",
        "payoutMethodId": 1,
        "payoutOptionId": 1,
        "currency": "INR",
        "currencyAmount": 2400,
        "payoutAmount": 10,
        "payoutDetails": "test@upi",
        "status": "PENDING",
        "requestedAt": "2026-01-10T12:00:00",
        "processedAt": null
      }
    ],
    "page": 1,
    "limit": 20,
    "totalElements": 1
  }
}
```

**Errors:** `401`

### 7.3 Get Withdrawal — `GET /api/withdrawals/{withdrawalId}`

Returns a withdrawal only if it belongs to the authenticated user.

```json
{
  "success": true,
  "message": "Withdrawal retrieved successfully",
  "data": {
    "withdrawalId": "WD-10001",
    "payoutMethodId": 1,
    "payoutOptionId": 1,
    "currency": "INR",
    "currencyAmount": 2400,
    "payoutAmount": 10,
    "payoutDetails": "test@upi",
    "status": "PENDING",
    "rejectionReason": null,
    "reviewNote": null,
    "transactionId": "TXN-10002",
    "requestedAt": "2026-01-10T12:00:00",
    "processedAt": null
  }
}
```

**Errors:** `401` · `403` belongs to another user · `404` not found

### 7.4 Move to Processing — `PATCH /api/withdrawals/{withdrawalId}/processing` *(ROLE_ADMIN)*

Transition: `PENDING → PROCESSING`. No request body.
**Response `data`:** `withdrawalId`, `status: "PROCESSING"`
**Errors:** `400` invalid state · `401` · `403` · `404`

### 7.5 Approve — `PATCH /api/withdrawals/{withdrawalId}/approve` *(ROLE_ADMIN)*

Transitions: `PENDING → APPROVED` or `PROCESSING → APPROVED`.
Approval performs **no additional wallet deduction** — VES was already deducted at creation.
**Response `data`:** `withdrawalId`, `status: "APPROVED"`, `processedAt`
**Errors:** `400` not approvable · `401` · `403` · `404`

### 7.6 Reject — `PATCH /api/withdrawals/{withdrawalId}/reject` *(ROLE_ADMIN)*

Transitions: `PENDING → REJECTED` or `PROCESSING → REJECTED`.
Reverses the original VES deduction.

**Request (reason required):**

```json
{ "rejectionReason": "Invalid payout details" }
```

**Reversal ledger entry:**

| Field | Value |
|-------|-------|
| Transaction type | `CORRECTION` |
| Source | `WITHDRAWAL_REJECTED` |
| Reference | `<withdrawalId>-REVERSAL` |

**Response `data`:** `withdrawalId`, `status: "REJECTED"`, `rejectionReason`, `processedAt`
**Errors:** `400` missing reason / invalid state / validation failure · `401` · `403` · `404`

### 7.7 Cancel — `PATCH /api/withdrawals/{withdrawalId}/cancel` *(Owner only)*

Transition: `PENDING → CANCELLED`. Not allowed once the withdrawal has moved beyond `PENDING`. No request body. Reverses the VES deduction.

**Reversal ledger entry:**

| Field | Value |
|-------|-------|
| Transaction type | `CORRECTION` |
| Source | `WITHDRAWAL_CANCELLED` |
| Reference | `<withdrawalId>-CANCELLATION-REVERSAL` |

**Response `data`:** `withdrawalId`, `status: "CANCELLED"`
**Errors:** `400` not in `PENDING` · `401` · `403` not the owner · `404`

---

## 8. Withdrawal Lifecycle

```
PENDING ──► PROCESSING ──► APPROVED
   │            │
   │            └────────► REJECTED
   ├──────────────────────► APPROVED
   ├──────────────────────► REJECTED
   └──────────────────────► CANCELLED
```

| Status | Description |
|--------|-------------|
| `PENDING` | Created; VES deducted |
| `PROCESSING` | Admin has started processing |
| `APPROVED` | Approved; no further deduction |
| `REJECTED` | Rejected; VES reversed |
| `CANCELLED` | Cancelled by user; VES reversed |

## 9. Balance Behavior

Deduction is **immediate** at creation and reversed on rejection or cancellation.

| Step | Wallet (VES) | Ledger entry |
|------|--------------|--------------|
| Initial | 10,000 | — |
| Withdrawal created | 7,600 | `WITHDRAWAL` −2,400 |
| Rejected or cancelled | 10,000 | `CORRECTION` +2,400 |
| Approved | 7,600 (unchanged) | — |

---

## 10. Security & Authorization

**Ownership:** The backend compares the JWT user ID with the withdrawal's user ID; a mismatch returns `403`. This applies to `GET /api/withdrawals/{id}` and `PATCH .../cancel`.

**Admin-only operations (`ROLE_ADMIN`):** wallet credit, wallet debit, and withdrawal `processing`, `approve`, `reject`. Normal users receive `403`.

**Security rules:**

- Wallet balances are backend-controlled; negative balances are prevented.
- User identity comes from the JWT; client-provided `userId` is never trusted for ownership.
- Users cannot directly credit or debit wallets.
- Payout values come from backend configuration; clients cannot specify arbitrary amounts.
- Rejection and cancellation reverse the original deduction.
- All wallet-affecting operations create ledger transactions.
- Wallet and ledger operations are transactional, and optimistic locking guards against concurrent wallet modifications.

---

## 11. Error Handling

Errors are handled globally and use the same wrapper with `success: false` and `data: null`.

| Scenario | HTTP | Message |
|----------|------|---------|
| Validation error | 400 | `Validation failed` |
| Insufficient balance | 400 | `Insufficient VES balance` |
| Invalid withdrawal state | 400 | `Invalid withdrawal state` |
| Unauthenticated | 401 | `Authentication required` |
| Access denied | 403 | `Access denied` |
| Ownership violation | 403 | `You are not authorized to access this withdrawal` |
| Withdrawal not found | 404 | `Withdrawal not found` |

```json
{ "success": false, "message": "Insufficient VES balance", "data": null }
```

**Business exceptions:** `InvalidWithdrawalRequestException`, `InvalidWithdrawalStateException`, `WithdrawalNotFoundException`, `WithdrawalOwnershipException`, `InsufficientBalanceException`

---

## 12. End-to-End Example

1. **Fetch configuration:** `GET /api/payouts/configuration`
2. **Create withdrawal:** `POST /api/withdrawals` with `payoutMethodId`, `payoutOptionId`, `payoutDetails` → backend resolves ₹10 = 2,400 VES, deducts it, writes the `WITHDRAWAL` ledger entry, and creates a `PENDING` withdrawal.
3. **View withdrawal:** `GET /api/withdrawals/WD-10001`
4. **Admin processing:** `PATCH /api/withdrawals/WD-10001/processing` → `PROCESSING`
5. **Admin approval:** `PATCH /api/withdrawals/WD-10001/approve` → `APPROVED` (no additional deduction)

**Alternate endings:** the admin rejects (`/reject` with a reason) or the user cancels while `PENDING` (`/cancel`). In both cases the wallet returns to 10,000 VES via a `CORRECTION` ledger entry.

---

## 13. Frontend Integration Rules

The React frontend should:

- Authenticate the user and handle the JWT per the application's security design.
- Fetch wallet and payout configuration from the backend and display backend-provided values.
- Submit only `payoutMethodId`, `payoutOptionId`, and `payoutDetails` when creating a withdrawal.
- Refresh wallet data after every withdrawal operation.
- Display backend validation errors.

The frontend must **never** calculate the VES deduction as the source of truth, submit an arbitrary amount, or modify wallet balances directly.

---

## 14. Architecture

```
Client ── JWT ──► Spring Security ──► REST Controllers
                                          │
                 ┌────────────────────────┼───────────────────────┐
                 ▼                        ▼                       ▼
           Wallet Service       Payout Config Service     Withdrawal Service
                 │                        │                       │
           Wallet Repository      Payout Repository     Withdrawal Repository
                 └────────────────────────┼───────────────────────┘
                                          ▼
                                 MySQL (managed by Flyway)
```

Financial decisions are made in backend services and never trusted from the frontend.

---

## 15. Verified Baseline

**Checkpoint:** `V3-WITHDRAWAL-SYSTEM-COMPLETE`

```
Tests run: 52 | Failures: 0 | Errors: 0 | Skipped: 0 | BUILD SUCCESS
Withdrawal integration tests: 14/14 PASS
```

**Verified areas:** authentication and JWT, authorization, wallet retrieval / transactions / summary / credit / debit, payout configuration, withdrawal creation / deduction / ledger / processing / approval / rejection / reversal / cancellation, ownership, and state validation.

Before continuing Phase 7, run `mvnw clean test` and confirm the results above. Phase 7 should **extend** this verified baseline, not rebuild it.

---

## 16. Phase 7 — Idempotency + Withdrawal Concurrency (Planned)

**Primary affected API:** `POST /api/withdrawals`

**Idempotency (concept):** the client sends an `Idempotency-Key`; if a request with that key already exists, return the existing result, otherwise process the withdrawal. The exact header name and persistence design will be finalized during implementation.

**Concurrency requirement:** concurrent requests must never cause duplicate deductions or a negative balance. Example: with a 10,000 VES balance, two simultaneous 8,000 VES requests must not both succeed.

**Planned test coverage:**

- Duplicate withdrawal requests
- Repeated idempotency keys
- Different idempotency keys
- Concurrent withdrawal requests
- Insufficient balance under concurrency
- Duplicate wallet deduction and duplicate withdrawal prevention
- Ledger consistency
- Safe retries