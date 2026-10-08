VELoop Rewards Backend — Testing
Purpose
Testing protects financial correctness while security, concurrency, fraud, idempotency, reconciliation, and observability evolve.
Layers
Unit
  -> Service/component
  -> API/controller
  -> Integration
  -> Database/concurrency
Unit tests
Important targets:
- WalletService
- WithdrawalService
- payout validators
- fraud rules and FraudRiskService
- RateLimitService
- GlobalExceptionHandler
- ReconciliationService
- ObservabilityMetrics
Wallet tests
Verify:
- credit changes balance
- credit creates ledger
- debit changes balance
- debit creates ledger
- insufficient balance is rejected
- currencies are handled correctly
- concurrent updates cannot silently overwrite state
Withdrawal tests
Verify:
- normal creation
- invalid method
- invalid option
- invalid details
- insufficient balance
- ownership
- invalid state transitions
- processing
- approval
- rejection/reversal
- cancellation/reversal
Idempotency tests
Verify:
- same key + same request returns existing withdrawal
- same key + different request is rejected
- missing/invalid key is rejected
- concurrent duplicates cannot produce duplicate financial operations
Concurrency integration
Use real persistence where necessary to prove:
two concurrent withdrawals
+
limited wallet balance
=
cannot both spend the same funds
Rate-limit tests
Cover:
- allowed
- blocked
- 429
- disabled rate limiting
- invalid configuration
- Redis failure/fail-open
- distributed behavior
- blocked metric
Fraud tests
The project evaluates rules such as:
Rapid Withdrawal
Repeated Withdrawal
Repeated Failed Request
Unusual Wallet Activity
Suspicious Payout Request
Suspicious Account Activity
Multiple Suspicious Payout Pattern
Each rule should be tested independently and then through the combined risk service.
Reconciliation tests
Verify:
match -> success
mismatch -> failure
mismatch -> audit
Security tests
Verify:
missing/invalid JWT -> 401
insufficient authorization -> 403
User A cannot access User B wallet
User A cannot access User B withdrawal
Audit tests
Verify expected events such as:
WALLET_CREDIT
WALLET_DEBIT
WITHDRAWAL_CREATED
WITHDRAWAL_PROCESSING
WITHDRAWAL_APPROVED
WITHDRAWAL_REJECTED
Observability tests
Verify:
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
Also verify X-Correlation-ID and X-Request-ID generation/propagation.
Regression workflow
implement
 -> targeted tests
 -> fix
 -> full suite
 -> inspect regression result
 -> commit
Financial invariants
The strongest tests verify:
1. Every successful wallet mutation has a ledger record.
2. Rejected withdrawal restores value through a correction transaction.
3. Cancelled eligible withdrawal restores value through a correction transaction.
4. Duplicate withdrawal does not duplicate the debit.
5. Concurrent withdrawals cannot overspend the wallet.
6. Users cannot access another user's financial records.
7. Wallet and ledger reconcile.
Current baseline
The latest project context reported:
235 / 235 tests passed
This should be rerun after any runtime code change. Phase 02.10 itself is documentation-only.
Completion
CHECKPOINT-02.10
ARCHITECTURE-DOCUMENTATION-COMPLETE