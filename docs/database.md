# VELoop Rewards Backend — Database

## 1. Database Technology

- MySQL 8.x
- Spring Data JPA / Hibernate
- Flyway
- `spring.jpa.hibernate.ddl-auto=none`

The schema is migration-controlled. Application startup validates and applies Flyway migrations rather than allowing Hibernate to create financial tables implicitly.

## 2. Migration History

Current migrations:

```text
V1__create_users.sql
V2__create_wallets.sql
V3__create_wallet_transactions.sql
V4__create_payout_configuration.sql
V5__create_withdrawals.sql
V6__create_withdrawal_idempotency.sql
V7__create_withdrawal_audit.sql
V8__create_audit_log.sql
V9__create_fraud_risk_events.sql
V10__create_authentication_attempts.sql
V11__add_explanation_to_fraud_risk_events.sql
V12__create_payout_jobs.sql
```

Existing migrations should not be rewritten casually. New schema changes should normally be introduced with a new migration.

## 3. Entity Relationship Overview

```text
users
  |
  +---- wallets (one per user)
  |
  +---- wallet_transactions
  |
  +---- withdrawals
  |          |
  |          +---- payout_methods
  |          +---- payout_options
  |          +---- wallet_transactions
  |          +---- payout_jobs
  |
  +---- withdrawal_idempotency
  +---- withdrawal_audit
  +---- audit_log
  +---- fraud_risk_events
  +---- authentication_attempts

payout_methods
      |
      +---- payout_options
```

## 4. Users

Table: `users`

Important columns:

```text
id
email
password_hash
name
role
account_status
verified
level
current_rank
created_at
updated_at
```

`email` is unique.

Passwords are stored as hashes, not plaintext.

## 5. Wallets

Table: `wallets`

```text
id
user_id
ves
sves
gems
tokens
spins
withdrawn_ves
created_at
updated_at
version
```

`user_id` is unique, enforcing one wallet per user.

The `version` field is used for optimistic locking.

Balances use `DECIMAL(19,4)`.

## 6. Wallet Ledger

Table: `wallet_transactions`

```text
id
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

Important database constraints/indexes include:

- unique `transaction_id`
- user index
- wallet index
- reference index
- created-at index
- status index
- foreign keys to `users` and `wallets`

The ledger is historical and must not be deleted merely to reverse a financial event.

## 7. Payout Configuration

Tables:

```text
payout_methods
payout_options
```

`payout_methods` contains:

```text
id
code
name
active
created_at
updated_at
```

The method code is unique.

`payout_options` contains:

```text
id
method_id
payout_amount
currency
currency_amount
active
created_at
updated_at
```

The option belongs to exactly one payout method and stores backend-controlled payout values.

## 8. Configured Payout Methods

The initializer maintains:

| Code | Name | State |
|---|---|---|
| `UPI` | UPI | Active |
| `AMAZON_GIFT_CARD` | Amazon Gift Card | Active |
| `GOOGLE_PLAY_GIFT_CARD` | Google Play Gift Card | Active |
| `PAYPAL` | PayPal | Inactive |

PayPal remains inactive until an appropriate provider integration is enabled.

## 9. Standard Payout Options

The configured INR payout schedule is:

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

The backend derives these values from the selected `payoutOptionId`.

## 10. Withdrawals

Table: `withdrawals`

```text
id
withdrawal_id
user_id
payout_method_id
payout_option_id
currency
currency_amount
payout_amount
payout_details
status
rejection_reason
review_note
transaction_id
requested_at
processed_at
created_at
updated_at
```

`withdrawal_id` is unique.

Useful indexes include user, status, created-at, user+status, and payout-option indexes.

## 11. Idempotency

Table: `withdrawal_idempotency`

```text
id
user_id
idempotency_key
request_fingerprint
withdrawal_record_id
created_at
updated_at
```

The pair `(user_id, idempotency_key)` is unique.

This prevents the same logical withdrawal from creating multiple financial operations.

## 12. Withdrawal Audit

Table: `withdrawal_audit`

```text
id
withdrawal_id
user_id
action
old_status
new_status
performed_by
description
created_at
```

Indexes cover withdrawal, user and created time.

## 13. Generic Audit

Table: `audit_log`

```text
id
actor_id
target_user_id
target_type
action
reference_id
metadata
created_at
```

This table records sensitive business/security events independently from withdrawal-specific lifecycle history.

## 14. Fraud Risk Events

Table: `fraud_risk_events`

```text
id
user_id
withdrawal_id
risk_score
decision
review_status
triggered_rules
explanation
reviewed_by
reviewed_at
review_note
created_at
```

Indexes cover user, withdrawal, decision, review status and creation time.

## 15. Authentication Attempts

Table: `authentication_attempts`

```text
id
user_id
email
success
created_at
```

Indexes support user, email, success/time and user+success+time analysis.

## 16. Payout Jobs

Table: `payout_jobs`

```text
id
withdrawal_id
status
attempt_count
next_attempt_at
last_error
created_at
updated_at
```

`withdrawal_id` is unique so one withdrawal maps to one payout job.

Indexes support status, next-attempt time and status+next-attempt retrieval.

## 17. Financial Integrity Rules

1. Never trust frontend balances.
2. Never trust a frontend-supplied withdrawal amount.
3. Preserve ledger history.
4. Use compensating `CORRECTION` transactions for reversals.
5. Keep wallet mutation and its ledger record in the same financial transaction boundary.
6. Enforce ownership and foreign-key relationships in persistence.
7. Use optimistic locking for concurrent wallet updates.
8. Reconcile stored wallet state against ledger-derived state.