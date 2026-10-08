# VELoop Rewards Backend — Financial Reconciliation

## 1. Purpose

Reconciliation verifies that the stored wallet balance agrees with the balance derived from completed wallet ledger transactions.

This provides an independent financial-integrity check after wallet mutations, withdrawals, reversals and concurrency events.

## 2. Endpoint

```http
GET /api/admin/reconciliation/wallet/{walletId}?currency=VES
Authorization: Bearer <ADMIN-JWT>
```

The endpoint is restricted to administrators.

## 3. Comparison

```text
Stored Wallet Balance
        vs
Ledger-Derived Balance
```

The expected invariant is equality for the selected wallet and currency.

## 4. Flow

```text
Admin request
   ↓
Load wallet
   ↓
Load authoritative completed ledger transactions
   ↓
Derive balance
   ↓
Compare
   |
   +-- match -> reconciliation.success
   |
   +-- mismatch -> reconciliation.failure + audit
```

## 5. Authoritative Sources

Reconciliation uses the wallet and wallet-transaction repositories as the financial sources.

Fraud-risk-event history is not a substitute for wallet ledger history.

## 6. Reversal Handling

Rejected/cancelled withdrawals preserve the original debit and add a compensating `CORRECTION` credit.

Therefore reconciliation includes both entries when deriving the effective balance.

## 7. Failure Handling

A mismatch is detected and recorded; the service does not silently overwrite the wallet merely to make the reconciliation pass.

The failure should remain auditable so the underlying cause can be investigated.

## 8. Investigation Workflow

For a mismatch:

```text
1. Identify wallet and currency
2. Inspect stored wallet balance
3. Derive ledger balance
4. Inspect recent ledger entries
5. Inspect withdrawal and reversal entries
6. Inspect concurrency/retry failures
7. Inspect audit records
8. Determine the source of divergence
9. Apply an explicitly controlled correction if required
10. Rerun reconciliation
```

## 9. Audit

A reconciliation mismatch is audited so the event does not disappear into an application log only.

## 10. Observability

Metrics:

```text
reconciliation.success
reconciliation.failure
```

## 11. Testing

Required verification includes:

- matching wallet and ledger -> success
- mismatch -> failure
- mismatch -> audit
- persisted integration behavior for the failure/audit path

## 12. Principle

Reconciliation detects accounting integrity defects. It does not replace transactions, locking, idempotency, validation or audit logging.