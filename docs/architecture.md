# VELoop Rewards Backend — Architecture

## 1. Purpose

VELoop Rewards is an independently developed backend for a rewards wallet and withdrawal system. The backend is the authoritative source for wallet balances, ledger transactions, payout configuration, withdrawals, authorization, idempotency, fraud/risk decisions, audit records, reconciliation, and operational observability.

The implementation is a demonstration/development system and must not use VELoop production credentials or production databases.

## 2. Technology Stack

| Area | Implementation |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| API | Spring Web / REST |
| Security | Spring Security |
| Authentication | JWT via JJWT 0.12.6 |
| Password hashing | BCrypt |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8.x |
| Schema migration | Flyway |
| Cache/shared rate-limit state | Redis |
| Validation | Jakarta Bean Validation |
| API documentation | Springdoc OpenAPI |
| Monitoring | Spring Boot Actuator + application metrics |
| Build | Maven |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, integration tests |

## 3. High-Level Architecture

```text
Client / Frontend
        |
        | REST + JWT
        v
Spring Security / Filters
        |
        v
Controllers
        |
        v
Domain Services
  |      |       |       |       |
 Auth   Wallet  Payout  Fraud  Reconciliation
        |         |
        |      Withdrawal
        |      /     \
        | Idempotency Audit
        |
        v
Repositories / JPA
        |
        v
      MySQL
        ^
      Flyway

Redis -> distributed rate limiting
Actuator -> health and metrics
Structured logs -> request/correlation tracing
```

## 4. Layer Responsibilities

### Controllers

Controllers provide HTTP routing, request binding, Jakarta validation, authentication-context access, role checks, and standard API responses.

### Services

Services contain business rules and transactional orchestration for authentication, wallet accounting, withdrawals, payouts, fraud/risk, idempotency, audit, reconciliation, and operational metrics.

### Repositories

Spring Data JPA repositories persist domain state and provide the authoritative historical queries used by financial and fraud logic.

### Entities

Entities represent persisted users, wallets, wallet transactions, payout configuration, withdrawals, idempotency records, audit records, fraud-risk events, authentication attempts, and payout jobs.

## 5. Request Flow

```text
HTTP request
   -> security filters
   -> JWT authentication
   -> authorization
   -> controller
   -> request validation
   -> domain service
   -> business validation / idempotency / fraud where applicable
   -> transactional persistence
   -> MySQL
```

## 6. Wallet Flow

```text
Authenticated request
        -> WalletService
        -> validate currency + amount
        -> load wallet
        -> apply atomic balance mutation
        -> persist wallet
        -> create wallet_transactions ledger record
        -> audit sensitive operation
```

Wallet state includes VES, SVES, GEMS, TOKENS, SPINS and withdrawn VES.

Every successful wallet mutation is expected to have a corresponding ledger entry.

## 7. Withdrawal Flow

```text
POST /api/withdrawals
        -> authenticated user
        -> idempotency key validation
        -> request fingerprint
        -> user/eligibility validation
        -> payout method validation
        -> payout option validation
        -> payout-detail validation
        -> fraud/risk evaluation
        -> if BLOCK: persist blocked risk event and reject
        -> if REVIEW: create PENDING review withdrawal without initial debit
        -> if ALLOW: resolve required VES and payout amount
        -> debit wallet
        -> link withdrawal to ledger transaction
        -> create PENDING withdrawal
        -> audit + idempotency + fraud event
```

The implemented normal ALLOW path uses immediate VES deduction when the withdrawal is created.

A REVIEW withdrawal is intentionally retained for review and does not receive the normal debit until an authorized approval operation processes it.

## 8. Withdrawal Processing Architecture

The project also contains a database-backed payout job model:

```text
Withdrawal
    |
    v
PayoutJob
    |
    +--> PayoutProviderRegistry
              |
              +--> PayoutProvider
              |
              +--> provider result

PayoutJob states:
QUEUED -> PROCESSING -> COMPLETED
               |
               +-> RETRY -> COMPLETED / FAILED
```

The job is uniquely associated with a withdrawal, uses pessimistic locking when loading a job for processing, and stores attempt count, next retry time, and the last error.

The retry policy allows up to 3 attempts with increasing delay beginning at 60 seconds.

No production payout provider integration or automatic scheduler is assumed by this documentation; provider execution is represented through the provider abstraction and worker implementation.

## 9. Concurrency and Financial Safety

Wallets use optimistic locking through a version column. Financial correctness is further protected by:

- database transactions
- database uniqueness constraints
- idempotency records
- balance validation
- ownership checks
- pessimistic locking for payout-job processing
- compensating ledger transactions for reversal

## 10. Security Architecture

```text
JWT authentication
      +
Role authorization
      +
Resource ownership
      +
Validation
      +
Rate limiting
      +
Idempotency
      +
Fraud/risk controls
      +
Audit logging
```

CSRF is disabled because the API is stateless and token-based. Form login and HTTP Basic authentication are not part of the API model.

## 11. Observability Architecture

The backend exposes Actuator health endpoints and application metrics. Structured console logging is configured in Logstash/structured JSON format.

Request tracing is supported through `X-Correlation-ID` and `X-Request-ID`.

## 12. Architectural Principles

1. The backend owns all financial state.
2. JWT identity is authoritative for user-owned resources.
3. Payout values are resolved from backend configuration.
4. Successful wallet mutations create ledger records.
5. Reversals are compensating transactions, not history deletion.
6. Duplicate financial requests are controlled by idempotency.
7. Concurrent financial updates are protected.
8. Sensitive actions are audited.
9. Fraud decisions are calculated from authoritative activity data.
10. Reconciliation independently verifies wallet-vs-ledger consistency.
11. Flyway owns schema evolution.
12. The architecture remains suitable for later frontend integration without moving financial logic to the client.