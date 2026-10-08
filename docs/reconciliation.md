VELoop Rewards Backend — Reconciliation
Purpose
Reconciliation verifies that stored wallet balances agree with balances derived from the wallet ledger.
Comparison
Stored Wallet Balance
        vs
Ledger-Derived Balance
The expected invariant is equality for each relevant wallet/currency.
Flow
start
 -> load wallet
 -> derive ledger balance
 -> compare
      |-- match -> reconciliation.success
      `-- mismatch -> reconciliation.failure + audit
Success
Matching balances record:
reconciliation.success
Failure
A mismatch records:
reconciliation.failure
and creates an audit record.
The service should not silently overwrite the wallet just to make the check pass.
Authoritative sources
Reconciliation uses the actual wallet and wallet-transaction repositories. Fraud-event records are not a substitute for financial ledger data.
Reversals
Rejected/cancelled withdrawals retain their original debit and add a compensating CORRECTION transaction. Reconciliation therefore accounts for both entries.
Investigation
For a mismatch:
1. identify wallet
2. inspect stored balance
3. derive ledger balance
4. inspect recent transactions
5. inspect withdrawal/reversal entries
6. inspect concurrency failures
7. inspect audit records
8. identify source of divergence
9. apply a controlled correction if required
10. rerun reconciliation
Audit
A failure should retain enough context to identify the affected financial state and the mismatch.
Testing
Required cases:
- matching wallet/ledger -> success
- mismatch -> failure
- mismatch -> audit
- integration persistence of failure/audit
Observability
Metrics:
reconciliation.success
reconciliation.failure
Principle
Reconciliation detects integrity defects; it does not replace transactions, locking, idempotency, or audit logging.