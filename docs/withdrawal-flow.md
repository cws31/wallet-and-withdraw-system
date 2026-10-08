# VELoop Rewards Backend — Withdrawal Flow

## 1. Purpose

Withdrawal is the financial path that converts an authenticated user's reward balance into a configured payout request.

The client selects a payout method and option, supplies payout details, and sends an idempotency key. The backend resolves the financial values and owns the complete operation.

## 2. Create Endpoint

```http
POST /api/withdrawals
Authorization: Bearer <JWT>
Idempotency-Key: <unique-key>
Content-Type: application/json
```

Request:

```json
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "user@upi"
}
```

`payoutMethodId`, `payoutOptionId`, and `payoutDetails` are validated server-side.

The request does not contain an authoritative VES amount.

## 3. Creation Pipeline

```text
JWT authentication
        ↓
USER authorization
        ↓
Idempotency key validation
        ↓
Request fingerprint
        ↓
Existing-key lookup
        ↓
User existence + withdrawal eligibility
        ↓
Payout method lookup
        ↓
Payout option lookup
        ↓
Method/option relationship validation
        ↓
Payout details validation
        ↓
Fraud/risk evaluation
        ↓
Decision
```

## 4. Fraud Decision Paths

### BLOCK

```text
Fraud decision = BLOCK
        ↓
Persist blocked fraud-risk event
        ↓
Reject request
        ↓
No withdrawal created
        ↓
No normal wallet debit
```

### REVIEW

```text
Fraud decision = REVIEW
        ↓
Create PENDING withdrawal
        ↓
Store review note
        ↓
Create audit/risk/idempotency records
        ↓
No initial normal VES debit
```

An authorized approval later performs the required VES debit when the withdrawal still has no linked transaction.

### ALLOW

```text
Fraud decision = ALLOW
        ↓
Resolve payout values
        ↓
Debit required VES immediately
        ↓
Create ledger transaction
        ↓
Create PENDING withdrawal
        ↓
Create audit/risk/idempotency records
```

## 5. Payout Value Resolution

The backend loads the selected payout option and uses:

```text
payout option payout_amount
payout option currency
payout option currency_amount
```

The client cannot replace the server-side required VES value.

Configured schedule:

| Payout | Required VES |
|---:|---:|
| ₹10 | 2,400 |
| ₹25 | 5,800 |
| ₹50 | 10,000 |
| ₹100 | 19,500 |
| ₹150 | 28,500 |
| ₹300 | 52,500 |
| ₹500 | 80,500 |
| ₹1,000 | 150,000 |

## 6. Withdrawal Statuses

```text
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
```

## 7. Lifecycle

```text
                  ┌──> PROCESSING ──> APPROVED
                  │          │
PENDING ──────────┤          └────────> REJECTED
   │              │
   ├──────────────┴────────────> REJECTED
   │
   └───────────────────────────> CANCELLED
```

The service enforces the state guards. The primary public cancellation path is `PENDING -> CANCELLED`.

## 8. Processing

```http
PATCH /api/withdrawals/{withdrawalId}/processing
```

Admin only.

Only `PENDING` withdrawals can be moved to `PROCESSING` through this endpoint.

## 9. Approval

```http
PATCH /api/withdrawals/{withdrawalId}/approve
```

Admin only.

Approval accepts `PENDING` or `PROCESSING` withdrawals.

For a normal ALLOW withdrawal, no second debit is made because the original `WITHDRAWAL` ledger transaction already exists.

For a REVIEW withdrawal without a linked transaction, approval performs the required VES debit and creates the linked transaction before marking the withdrawal `APPROVED`.

## 10. Rejection

```http
PATCH /api/withdrawals/{withdrawalId}/reject?rejectionReason=...&reviewNote=...
```

Admin only.

`rejectionReason` is required.

A withdrawal in `PENDING` or `PROCESSING` can be rejected.

For a normally debited withdrawal, the original VES debit is reversed through a compensating `CORRECTION` credit.

## 11. Cancellation

```http
PATCH /api/withdrawals/{withdrawalId}/cancel
```

Requires authenticated `USER` role and resource ownership.

Only `PENDING` withdrawals can be cancelled.

For a normally debited withdrawal, cancellation creates a compensating `CORRECTION` credit.

## 12. Ownership

User-facing withdrawal operations derive the user ID from the authenticated principal.

A user cannot read or cancel another user's withdrawal.

## 13. Idempotency

The same user + same idempotency key + same request fingerprint returns the previously created withdrawal.

The same key with a different request is rejected.

## 14. Concurrency

Withdrawal safety depends on:

- database transaction boundaries
- wallet optimistic locking
- idempotency uniqueness
- sufficient-balance checks
- withdrawal state guards

## 15. Audit and Risk Records

Withdrawal lifecycle operations produce withdrawal-specific audit records and generic audit events where applicable.

Fraud evaluation produces `fraud_risk_events` records with score, decision, triggered rules and explanations.

## 16. Payout Processing

The project includes a database-backed `PayoutJob` model and a `PayoutWorker` with provider abstraction and retry support.

A payout job is unique per withdrawal and can move through:

```text
QUEUED
PROCESSING
RETRY
COMPLETED
FAILED
```

The retry policy supports up to three attempts.

## 17. Observability

Withdrawal lifecycle metrics include:

```text
withdrawal.created
withdrawal.processing
withdrawal.approved
withdrawal.rejected
withdrawal.cancelled
```