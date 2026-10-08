# VELoop Rewards Backend — Idempotency

## 1. Purpose

Idempotency prevents one logical withdrawal from becoming multiple financial operations when clients retry because of double clicks, network failures, timeouts or duplicated requests.

Primary endpoint:

```http
POST /api/withdrawals
```

## 2. Client Contract

Every withdrawal attempt should send:

```http
Idempotency-Key: <unique-value>
```

The HTTP controller accepts the header as optional for binding compatibility, but the service rejects a missing or blank key. Therefore the client contract is effectively required.

Keys are normalized and limited to 100 characters.

## 3. Flow

```text
Request
  ↓
Validate key
  ↓
Normalize key
  ↓
Create SHA-256 request fingerprint
  ↓
Lookup (user, key)
       |
       +-- not found --> process withdrawal
       |
       +-- found --> compare fingerprint
                         |
                         +-- same --> return existing withdrawal
                         |
                         +-- different --> reject request
```

## 4. Same Key + Same Request

The previously associated withdrawal is returned.

There should be:

```text
one logical withdrawal
one withdrawal record
one financial debit
one idempotency record
```

## 5. Same Key + Different Request

The request fingerprint differs.

The reused key is rejected instead of being allowed to represent two different financial operations.

## 6. Persistence

Table: `withdrawal_idempotency`

```text
id
user_id
idempotency_key
request_fingerprint
withdrawal_record_id
created_at
updated_at
```

The pair `(user_id, idempotency_key)` is protected by a database uniqueness constraint.

## 7. User Scope

The same key text can exist independently for different authenticated users because the uniqueness scope includes `user_id`.

## 8. Concurrency

Idempotency alone is not sufficient for financial safety.

It works together with:

- transaction boundaries
- wallet optimistic locking
- database uniqueness
- balance validation
- withdrawal state checks

## 9. Retry Rule

After a network timeout where the request may have reached the backend, retry the same logical withdrawal using the same idempotency key.

Do not create a fresh key solely because the previous response was not received.

## 10. Security

Idempotency does not replace:

```text
JWT authentication
authorization
fraud detection
rate limiting
ownership validation
concurrency protection
```

## 11. Financial Guarantee

The intended invariant is:

```text
Repeated identical request
        ↓
Same logical operation
        ↓
No second wallet deduction
```