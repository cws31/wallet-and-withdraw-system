# VELoop Rewards Backend — Rate Limiting

## 1. Purpose

Rate limiting controls request frequency for sensitive endpoints and uses Redis as shared state so limits can work across multiple backend instances.

## 2. Configuration

```properties
security.rate-limit.enabled=true

security.rate-limit.login-requests=5
security.rate-limit.login-window-seconds=60

security.rate-limit.withdrawal-requests=5
security.rate-limit.withdrawal-window-seconds=60

security.rate-limit.wallet-mutation-requests=20
security.rate-limit.wallet-mutation-window-seconds=60

security.rate-limit.withdrawal-mutation-requests=20
security.rate-limit.withdrawal-mutation-window-seconds=60

security.rate-limit.max-entries=10000
```

## 3. Limits

| Category | Maximum | Window |
|---|---:|---:|
| Login | 5 | 60 seconds |
| Withdrawal creation | 5 | 60 seconds |
| Wallet mutation | 20 | 60 seconds |
| Withdrawal mutation | 20 | 60 seconds |

## 4. Architecture

```text
HTTP request
      ↓
RateLimitFilter / RateLimitService
      ↓
Redis shared state
      |
      +-- under limit -> continue
      |
      +-- over limit  -> 429
```

## 5. Blocked Requests

A real configured violation returns:

```text
HTTP 429 Too Many Requests
```

The application also records:

```text
rate_limit.blocked
```

## 6. Fail-Open Behavior

Redis/infrastructure failures do not automatically become rate-limit blocks in the current implementation.

This is deliberate: an unavailable rate-limit backend should not incorrectly deny all application traffic.

## 7. Relationship to Other Controls

```text
Rate limiting -> limits request volume
Idempotency   -> prevents duplicate logical financial operations
Authorization -> checks permission
Fraud        -> evaluates suspicious behavior
Concurrency  -> protects financial race conditions
```

These controls solve different threat classes and are intentionally layered.

## 8. Testing Requirements

Rate-limiting tests should cover:

- allowed requests
- blocked requests
- 429 status
- disabled configuration
- invalid configuration
- Redis failure/fail-open behavior
- distributed behavior
- blocked metric

## 9. Security Principle

Rate limiting is an additional defense layer. It does not replace authentication, authorization, validation, or financial controls.