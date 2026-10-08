# VELoop Rewards Backend — Testing

## 1. Purpose

Testing protects financial correctness as wallet accounting, withdrawal processing, fraud/risk controls, security, idempotency, concurrency, reconciliation and observability evolve.

## 2. Test Layers

```text
Unit tests
   ↓
Service/component tests
   ↓
API/controller tests
   ↓
Integration tests
   ↓
Database/concurrency tests
```

## 3. Core Test Areas

### Authentication

Verify:

- registration validation
- duplicate email rejection
- password hashing
- successful login
- invalid credentials
- JWT issuance
- authenticated identity

### Wallet

Verify:

- credit changes balance
- credit creates ledger record
- debit changes balance
- debit creates ledger record
- insufficient balance is rejected
- all supported currencies are handled
- user isolation
- concurrent updates cannot silently overwrite financial state

### Payout

Verify:

- active payout methods are exposed
- inactive methods are hidden
- payout options are exposed from backend configuration
- all configured UPI options exist
- payout values match backend configuration
- method/option relationship is validated
- method-specific payout detail validation works
- inactive PayPal remains excluded

### Withdrawal

Verify:

- normal creation
- server-side payout resolution
- insufficient balance
- invalid method
- invalid option
- invalid payout details
- ownership
- processing
- approval
- rejection
- cancellation
- rejection reversal
- cancellation reversal
- valid/invalid state transitions

### Idempotency

Verify:

- same key + same request returns existing withdrawal
- same key + different request is rejected
- missing/blank key is rejected
- concurrent duplicate requests cannot create duplicate financial operations

### Concurrency

Use real persistence where appropriate to prove:

```text
Two concurrent withdrawals
        +
Limited wallet balance
        =
Cannot both spend the same funds
```

### Rate Limiting

Verify:

- requests below the limit pass
- excessive requests return 429
- configuration can be disabled
- invalid configuration is rejected
- Redis failure follows fail-open behavior
- distributed behavior is preserved
- blocked metric is recorded

### Fraud / Abuse Protection

The implemented rules are:

```text
Rapid Withdrawal
Repeated Withdrawal
Repeated Failed Request
Unusual Wallet Activity
Suspicious Payout Request
Suspicious Account Activity
Multiple Suspicious Payout Pattern
```

Verify rule-specific scoring and combined decisions for:

```text
ALLOW
REVIEW
BLOCK
```

Verify that blocked withdrawals do not create the normal withdrawal/debit path.

### Security

Verify:

```text
Missing/invalid JWT -> 401
Insufficient authorization -> 403
User A cannot access User B wallet
User A cannot access User B withdrawal
Client cannot choose arbitrary VES
Client cannot set the stored wallet balance
```

### Audit

Verify important events such as:

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

### Reconciliation

Verify:

```text
wallet = ledger -> reconciliation success
wallet != ledger -> reconciliation failure + audit
```

### Observability

Verify application metrics including:

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

Also verify `X-Correlation-ID` and `X-Request-ID` generation/propagation behavior.

## 4. Regression Workflow

```text
implement change
   ↓
targeted test
   ↓
fix defect if required
   ↓
full regression suite
   ↓
inspect final result
   ↓
commit
```

## 5. Automated Regression Baseline

Verified project baseline on 8 October 2026:

```text
257 / 257 tests passed
Failures: 0
Errors: 0
```

This is the current backend regression baseline after the implemented hardening and documentation work.

## 6. Manual API Verification

The verified manual flow covers:

```text
Login
  ↓
Current user
  ↓
Wallet
  ↓
Wallet summary
  ↓
Wallet transactions
  ↓
Payout configuration
  ↓
Create withdrawal
  ↓
Repeat with same idempotency key
  ↓
Withdrawal history/details
  ↓
Cancel eligible withdrawal
  ↓
Wallet reversal verification
  ↓
Ledger verification
  ↓
Reconciliation
```

## 7. Financial Invariants

The strongest tests maintain these invariants:

1. Every successful wallet mutation has a ledger record.
2. Insufficient balance never produces a successful debit.
3. Rejected normal withdrawals restore the debited VES through a `CORRECTION` transaction.
4. Cancelled normal withdrawals restore the debited VES through a `CORRECTION` transaction.
5. Duplicate withdrawal requests do not create duplicate financial operations.
6. Concurrent withdrawals cannot overspend the same wallet.
7. Users cannot access another user's financial records.
8. Wallet and ledger reconcile.

## 8. Test Completion

```text
CHECKPOINT-02.10
ARCHITECTURE-DOCUMENTATION-COMPLETE

AUTOMATED-REGRESSION
257/257 PASS

MANUAL-API-VERIFICATION
COMPLETED
```