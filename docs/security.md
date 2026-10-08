VELoop Rewards Backend — Security
Goals
Protect authentication, authorization, ownership, financial values, administrative operations, secrets, availability, and auditability.
Authentication
JWT authentication is used:
Authorization: Bearer <JWT>
Flow:
Login -> JWT -> authentication filter -> SecurityContext -> controller
Authorization
Authentication identifies the user. Authorization determines whether the user may perform an operation.
Administrative wallet and withdrawal lifecycle operations require administrative authorization. Normal users operate only on their own resources.
Ownership
Wallet and withdrawal access is derived from the authenticated identity. Client-supplied ownership identifiers must not override the security context.
Passwords
Passwords must be hashed and never stored, logged, or returned in plaintext.
Secrets
Database credentials and JWT secrets come from external configuration. Real .env files, passwords, JWT secrets, API keys, and production credentials must not be committed.
CORS
Allowed origins are configuration-driven. Production should use explicit trusted origins.
Rate limiting
Current limits:
Login:                 5 / 60 seconds
Withdrawal creation:   5 / 60 seconds
Wallet mutations:     20 / 60 seconds
Withdrawal mutations: 20 / 60 seconds
Redis provides distributed rate-limit state.
Input validation
Validation covers required fields, formats, payout configuration, payout details, amounts, ownership, withdrawal state, idempotency, and financial eligibility.
Financial security
The backend resolves the real payout and required VES from stored payout configuration. Frontend-supplied balances or financial amounts are not authoritative.
Fraud protection
Withdrawal requests pass through risk evaluation. The implemented risk layer can allow, review, or block requests.
Concurrency
Wallet optimistic locking prevents stale concurrent updates from silently overwriting financial state.
Audit
Sensitive operations are auditable, including wallet mutations and withdrawal lifecycle events.
Error security
Unexpected errors must not expose stack traces, credentials, SQL details, JWT secrets, or other internal implementation details.
Principles
1. Authenticate protected requests.
2. Authorize privileged actions.
3. Enforce ownership.
4. Hash passwords.
5. Externalize secrets.
6. Validate input.
7. Rate-limit sensitive APIs.
8. Use idempotency for retryable financial requests.
9. Protect concurrent updates.
10. Audit sensitive mutations.