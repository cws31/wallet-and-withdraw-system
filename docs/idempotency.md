VELoop Rewards Backend — Idempotency
Purpose
Idempotency prevents retries of one logical withdrawal from creating multiple financial operations.
Primary endpoint:
POST /api/withdrawals
Client contract
Idempotency-Key: <unique-value>
One logical withdrawal should reuse the same key when retrying.
Flow
Request
 -> validate key
 -> normalize key
 -> create request fingerprint
 -> lookup (user,key)
       |
       +-- not found -> process
       |
       +-- found -> compare fingerprint
                     |
                     +-- same -> return existing
                     |
                     +-- different -> conflict
Same key + same request
The existing withdrawal is returned. No second wallet debit is created.
Same key + different request
The fingerprint differs, so the request is rejected rather than reusing the original key for a different financial operation.
Persistence
withdrawal_idempotency stores:
user_id
idempotency_key
request_fingerprint
withdrawal_record_id
created_at
updated_at
The user/key combination is protected by a database uniqueness constraint.
User scope
Keys are scoped to the authenticated user. The same textual key used by another user is independent.
Concurrency
Idempotency is only one layer. Financial safety also depends on:
- transaction boundaries
- wallet optimistic locking
- database uniqueness
- balance validation
Retry rule
If a network timeout occurs after submission, retry the same logical withdrawal with the same key.
Do not create a new key merely because the first response was not received.
Security
Idempotency does not replace authentication, authorization, fraud detection, rate limiting, or concurrency control.