# VELoop Rewards Backend — Wallet Accounting

## 1. Accounting Model

The accounting model has two complementary views:

```text
Current Wallet State
        +
Historical Wallet Ledger
```

The current wallet provides fast balance access. The ledger provides the auditable history of every successful balance mutation.

## 2. Supported Currencies

```text
VES
SVES
GEMS
TOKENS
SPINS
```

## 3. Wallet Representation

The wallet stores:

```text
ves
sves
gems
tokens
spins
withdrawn_ves
version
created_at
updated_at
```

All financial/reward values use precise `BigDecimal` values and database `DECIMAL(19,4)` columns.

## 4. Credit Flow

```text
Request
 -> authentication/authorization
 -> request validation
 -> load authenticated user's wallet
 -> calculate new balance
 -> persist wallet
 -> create ledger transaction
 -> audit
```

For a credit:

```text
balance_after = balance_before + amount
```

## 5. Debit Flow

```text
Request
 -> authentication/authorization
 -> validation
 -> load wallet
 -> sufficient-balance check
 -> calculate new balance
 -> persist wallet
 -> create ledger transaction
 -> audit
```

For a debit:

```text
balance_after = balance_before - amount
```

An insufficient balance must prevent the successful debit.

## 6. Ledger Transaction Types

The source enum contains:

### Credit/reward types

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

### Debit/correction types

```text
WITHDRAWAL
EXCHANGE_DEBIT
ADMIN_DEBIT
CORRECTION
```

The Java `TransactionType` enum is authoritative if new transaction types are introduced.

## 7. Ledger Record

Every successful wallet mutation records:

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

## 8. Withdrawal Accounting

The normal ALLOW withdrawal path uses immediate VES deduction.

Example:

```text
25,000 VES
-2,400 VES withdrawal
----------------------
22,600 VES
```

The resulting withdrawal references the corresponding `WITHDRAWAL` ledger transaction.

## 9. Fraud-Review Accounting

When a withdrawal receives a `REVIEW` decision during creation:

```text
Fraud decision = REVIEW
       ↓
PENDING withdrawal created
       ↓
No normal initial VES debit
       ↓
Admin review
```

Approval of a review withdrawal performs the required VES debit before moving it to `APPROVED`.

## 10. Reversal Accounting

Rejected or cancelled withdrawals do not delete the original financial history.

```text
Original withdrawal debit
          +
Compensating CORRECTION credit
          =
Net zero economic effect
```

Typical rejection reference:

```text
<withdrawalId>-REVERSAL
```

Typical cancellation reference:

```text
<withdrawalId>-CANCELLATION-REVERSAL
```

## 11. Atomicity

Wallet mutation and ledger creation are coordinated through Spring transactions so a successful financial operation is not intentionally exposed without its corresponding ledger record.

## 12. Concurrency

The wallet entity uses JPA optimistic locking through the `version` field.

This prevents stale concurrent writes from silently overwriting newer financial state.

Concurrency safety is complemented by database uniqueness and transactional boundaries.

## 13. Accounting Invariant

Conceptually:

```text
Current Balance
=
Initial Balance
+ Credits
- Debits
+ Corrections
```

For a single currency, the stored wallet value should agree with the value derived from its completed ledger transactions.

## 14. Reconciliation

Reconciliation compares the stored wallet balance with the ledger-derived balance for the selected wallet/currency.

A mismatch is recorded and audited rather than silently hidden.

## 15. Accounting Rules

1. Frontend balances are never authoritative.
2. Frontend withdrawal amounts are never authoritative.
3. Required VES comes from backend payout configuration.
4. Every successful wallet mutation has a ledger record.
5. Financial history is never deleted as a reversal mechanism.
6. Reversals use compensating `CORRECTION` transactions.
7. Ownership is derived from the authenticated identity.
8. Precise decimal values are used for wallet accounting.
9. Reconciliation is an integrity check, not a substitute for proper transactions.