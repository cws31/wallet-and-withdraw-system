VELoop Rewards Backend — Database
Technology
- MySQL
- Spring Data JPA / Hibernate
- Flyway
- spring.jpa.hibernate.ddl-auto=none
Main relationships
User
 ├── Wallet
 ├── WalletTransaction
 ├── Withdrawal
 ├── WithdrawalIdempotency
 └── Audit/Fraud records

PayoutMethod
 └── PayoutOption
       └── Withdrawal
Wallets
Table: wallets
Important fields:
id, user_id, ves, sves, gems, tokens, spins,
withdrawn_ves, version, created_at, updated_at
user_id is unique, giving one wallet per user. version supports optimistic locking.
Wallet ledger
Table: wallet_transactions
Important fields:
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
The ledger preserves the history of financial mutations.
Payout configuration
Tables:
payout_methods
payout_options
Payout methods have a unique code. Payout options reference their method and contain backend-controlled payout/currency values and active state.
Withdrawals
Table: withdrawals
Important fields:
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
Idempotency
Table: withdrawal_idempotency
Important fields:
user_id
idempotency_key
request_fingerprint
withdrawal_record_id
created_at
updated_at
The user/key combination is unique.
Audit
Withdrawal-specific audit stores lifecycle transitions. Generic audit stores actor, target, action, reference, metadata, and timestamp.
Migration strategy
Flyway migrations create and evolve the schema. Existing migrations should not be edited casually; new schema changes should normally use a new migration.
Financial rule
Do not delete a ledger transaction to undo a financial event. Use a compensating transaction so history remains auditable.