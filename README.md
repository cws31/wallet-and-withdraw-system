# VELoop Rewards — Wallet & Withdrawal Backend

> **🚧 Development Status: IN PROGRESS**
>
> A secure, backend-driven Wallet & Withdrawal system for VELoop Rewards.
> This project is being developed incrementally with strict phase-based checkpoints.
>
> **Current Checkpoint:** `V1-AUTH-FOUNDATION`  
> **Current Phase:** Authentication, JWT, validation, exception handling and OpenAPI are completed.  
> **Next Phase:** Wallet Core

---

## 📌 Project Overview

VELoop Rewards Backend is a Spring Boot REST API designed to provide the backend foundation for a rewards wallet and withdrawal system.

The backend is the **single source of truth** for:

- User identity and authentication
- Wallet balances
- Reward transactions
- Payout configuration
- Withdrawal processing
- Security and authorization
- Audit and transaction history

The system is being built so that the frontend can never directly manipulate wallet balances or bypass backend validation.

### Current Implementation

The following foundation is implemented and tested:

- User registration
- Request validation
- BCrypt password hashing
- User login
- JWT access-token generation
- JWT authentication filter
- Protected APIs
- Authentication and authorization error handling
- Global exception handling
- Standard API response format
- OpenAPI / Swagger documentation

### Still Under Development

- Wallet core
- Wallet transaction ledger
- Reward credit/debit operations
- Payout configuration
- Withdrawal processing
- Idempotency
- Audit logging
- Rate limiting
- Additional production hardening
- React/Vite frontend
- Deployment and production infrastructure

---

## 🔖 Current Development Checkpoint

### `V1-AUTH-FOUNDATION`

This checkpoint represents the verified state of the project as of **25 September 2026**.

| Area | Status |
|---|---|
| Project setup | ✅ Completed |
| Java 21 target | ✅ Completed |
| Spring Boot 3.5.16 | ✅ Completed |
| Maven build | ✅ Verified |
| MySQL connection | ✅ Completed |
| Flyway | ✅ Completed |
| Users migration | ✅ Completed |
| User entity | ✅ Completed |
| User repository | ✅ Completed |
| Registration | ✅ Completed |
| Request validation | ✅ Completed |
| BCrypt password hashing | ✅ Completed |
| Login | ✅ Completed |
| JWT generation | ✅ Completed |
| JWT authentication filter | ✅ Completed |
| SecurityContext integration | ✅ Completed |
| Protected endpoint | ✅ Completed |
| 401 handling | ✅ Completed |
| 403 handling | ✅ Completed |
| Global exception handling | ✅ Completed |
| Standard API responses | ✅ Completed |
| OpenAPI specification | ✅ Completed |
| Swagger UI | ✅ Completed |
| JWT Bearer documentation | ✅ Completed |
| Authentication API testing | ✅ Completed |

### Checkpoint Rule

No future wallet, payout or withdrawal functionality is considered implemented at this checkpoint.

The next development work begins from:

> **PHASE 4 — WALLET CORE**

---

# 🛠️ Technology Stack

## Backend

- Java 21
- Spring Boot 3.5.16
- Maven
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JJWT 0.12.6
- Jakarta Bean Validation
- Flyway
- Springdoc OpenAPI
- Swagger UI

## Database

- MySQL 8.x

## Planned Testing & Quality

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers
- Postman
- Swagger UI

## Planned Frontend

- React
- Vite

---

# 🏗️ Architecture

```text
                    React + Vite
                         |
                         | REST + JWT
                         v
              +-----------------------+
              |    Spring Boot API    |
              +-----------------------+
                         |
              +----------+----------+
              |                     |
              v                     v
       Spring Security        Controllers
              |                     |
              v                     v
          JWT Filter             Services
                                    |
                                    v
                              Repositories
                                    |
                                    v
                           Spring Data JPA
                                    |
                                    v
                                MySQL 8.x
                                    ^
                                    |
                                  Flyway