VELoop Rewards Backend — Architecture
Purpose
VELoop Rewards is a backend-controlled rewards wallet and withdrawal system. The backend is the source of truth for balances, ledger transactions, payout configuration, withdrawals, authorization, idempotency, fraud controls, audit records, reconciliation, and observability.
High-level architecture
React/Web Client
      |
 REST + JWT
      v
Spring Boot API
      |
  +---+---+-------------------+
  |       |                   |
 Auth   Wallet              Payout
          |                   |
        Ledger          Configuration
          |
      Withdrawal
     /    |     \
Idempotency Fraud  Audit
      |
    MySQL
      ^
    Flyway

Redis -> distributed rate limiting
Actuator -> health and metrics
Structured logs -> request/correlation tracing
Layers
- Controllers: HTTP routing, binding, validation, response handling.
- Services: wallet, withdrawal, payout, fraud, idempotency, audit, reconciliation business logic.
- Repositories: Spring Data JPA persistence.
- Entities: persistent domain state.
- Security: JWT authentication, authorization, filters, CORS/security configuration.
- Infrastructure: MySQL, Flyway, Redis, Actuator.
Core request flow
Request
 -> Security filters
 -> JWT authentication
 -> Authorization
 -> Controller
 -> Service
 -> Validation / Idempotency / Fraud / Wallet / Audit
 -> Repository
 -> MySQL
Withdrawal flow
POST /api/withdrawals
 -> authenticate
 -> authorize
 -> validate idempotency
 -> validate user eligibility
 -> validate payout method/option/details
 -> fraud risk evaluation
 -> resolve required VES
 -> debit wallet
 -> create ledger transaction
 -> create PENDING withdrawal
 -> audit + idempotency record
The wallet is debited when the withdrawal is created.
Concurrency
Wallets use optimistic locking through a version field. Idempotency and database uniqueness provide additional duplicate/concurrency protection.
Principles
1. Backend owns financial state.
2. Every successful wallet mutation has a ledger record.
3. Financial values are resolved server-side.
4. JWT identity is authoritative for user-owned resources.
5. Duplicate withdrawals are controlled by idempotency.
6. Concurrent wallet updates are protected.
7. Reversals use compensating ledger transactions.
8. Sensitive operations are audited.
9. Schema changes use Flyway.
10. Monitoring is part of the application architecture.