VELoop Rewards Backend — Withdrawal Flow
Create
POST /api/withdrawals
Authorization: Bearer <JWT>
Idempotency-Key: <unique-key>
{
  "payoutMethodId": 1,
  "payoutOptionId": 1,
  "payoutDetails": "user@upi"
}
The client does not provide the authoritative amount to deduct.
Creation pipeline
JWT
 -> authorization
 -> idempotency
 -> user eligibility
 -> payout method
 -> payout option
 -> method/option validation
 -> payout-detail validation
 -> fraud/risk evaluation
 -> resolve required VES
 -> wallet debit
 -> ledger transaction
 -> PENDING withdrawal
 -> audit
 -> idempotency record
Statuses
PENDING
PROCESSING
APPROVED
REJECTED
CANCELLED
Lifecycle
                 -> APPROVED
                /
PENDING -> PROCESSING
   |                 |               -> REJECTED
   |
   +-> REJECTED
   |
   +-> CANCELLED
The exact state guards in WithdrawalService are authoritative.
Processing
PATCH /api/withdrawals/{withdrawalId}/processing
Administrative operation. Moves an eligible pending withdrawal to processing.
Approval
PATCH /api/withdrawals/{withdrawalId}/approve
Approval does not debit the wallet again.
Rejection
PATCH /api/withdrawals/{withdrawalId}/reject
Requires a rejection reason. The original debit is reversed with a compensating transaction.
Cancellation
PATCH /api/withdrawals/{withdrawalId}/cancel
Owner cancellation is allowed only in the eligible state and reverses the original debit.
Ownership
User withdrawal reads/operations are scoped to the authenticated user. Another user's withdrawal cannot be accessed through the normal user flow.
Payout configuration
The backend resolves method, option, payout amount, currency, required VES, active state, and method/option relationship.
Fraud
Withdrawal creation invokes the fraud-risk layer. A blocked request is rejected before the financial withdrawal is created; review decisions are retained according to the fraud implementation.
Idempotency
Same key + same request returns the existing withdrawal. Same key + different request is rejected. This prevents duplicate financial operations.
Concurrency
Safety comes from transactional wallet operations, optimistic locking, idempotency, and database constraints.
Observability
Lifecycle metrics include:
withdrawal.created
withdrawal.processing
withdrawal.approved
withdrawal.rejected
withdrawal.cancelled