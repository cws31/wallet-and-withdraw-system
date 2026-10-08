VELoop Rewards Backend — Rate Limiting
Purpose
Rate limiting controls request frequency for sensitive operations. Redis provides shared state for distributed application instances.
Configuration
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
Limits
Category	Maximum	Window
Login	5	60 seconds
Withdrawal creation	5	60 seconds
Wallet mutation	20	60 seconds
Withdrawal mutation	20	60 seconds


Architecture
HTTP request
 -> RateLimitFilter/Service
 -> Redis
     |-- under limit -> continue
     `-- over limit -> 429
Blocked requests
An actual limit violation returns:
429 Too Many Requests
The application records:
rate_limit.blocked
Fail-open behavior
Redis/infrastructure errors do not automatically become rate-limit blocks. The current service preserves fail-open behavior for infrastructure failures.
Relationship to other controls
Rate limiting -> request volume
Idempotency   -> duplicate logical operations
Authorization -> permission
Fraud         -> suspicious behavior
Concurrency   -> financial race conditions
These controls are complementary.
Testing
Verify:
- allowed requests
- blocked requests
- 429 status
- disabled configuration
- invalid configuration
- Redis failure/fail-open behavior
- distributed behavior
- blocked metric
Security principle
Rate limiting is an additional protection layer, not the primary authorization mechanism.