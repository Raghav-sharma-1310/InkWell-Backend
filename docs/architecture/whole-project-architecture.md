# Whole-Project Architecture

## Overview

InkWell is a **microservices-based blogging platform** built on Spring Boot 3.3 and Spring Cloud 2023. The architecture follows the API Gateway pattern with Eureka service discovery, event-driven messaging via RabbitMQ, and Redis caching at the gateway level.

## Architecture Layers

### 1. Client Layer
- **React SPA** (Vite + TailwindCSS) on port 5173
- Communicates exclusively through the API Gateway
- Stores JWT tokens in localStorage
- Role-based UI rendering (Reader / Author / Admin / Default Admin)

### 2. Edge Layer — API Gateway (port 8080)
- **Spring Cloud Gateway** (reactive, WebFlux-based)
- JWT validation via `JwtAuthenticationFilter` (GlobalFilter)
- Path-based role enforcement: PUBLIC, ADMIN, AUTHOR, PREMIUM paths
- Injects `X-User-Id`, `X-Username`, `X-User-Role`, `X-User-Email`, `X-User-Subscription-Tier`, `X-User-Subscription-Status` headers
- Circuit breaker (Resilience4j) with fallback controller
- Redis-backed rate limiting on newsletter endpoints
- CORS configuration for frontend origin
- Aggregated Swagger UI for all downstream services

### 3. Service Discovery Layer
- **Eureka Server** (port 8761) — all services register with `prefer-ip-address: true`
- Gateway uses `lb://` URIs for client-side load balancing
- Feign clients resolve service names through Eureka

### 4. Business Services Layer

| Service | Responsibilities |
|---|---|
| auth-service (8081) | User CRUD, JWT/refresh tokens, OAuth2 (Google/GitHub), OTP password reset, email verification, role management, subscription/payment processing, feedback, author requests, admin console |
| post-service (8082) | Post CRUD, publishing workflow, scheduling, likes, bookmarks, follows, reading history, view counting |
| category-service (8083) | Category/tag CRUD, post-taxonomy mapping, slug management |
| comment-service (8084) | Comment CRUD (threaded), comment likes, admin moderation |
| media-service (8085) | File upload (local filesystem or AWS S3), media metadata management |
| newsletter-service (8086) | Double opt-in subscription, campaign management, email delivery |
| notification-service (8087) | In-app notifications, email notifications, RabbitMQ event consumers, audit logging, admin broadcasts |
| payment-service (8088) | Razorpay order creation |

### 5. Infrastructure Layer
- **MySQL 8.4** — separate schemas per service (auth_db, post_db, etc.)
- **Redis 7.4** — gateway rate limiting, post-service caching
- **RabbitMQ 3.13** — async event delivery (post published, comment created, post deleted)
- **Mailpit** — email capture in development (SMTP on 1025, UI on 8025)
- **Spring Boot Admin** (port 9090) — health monitoring dashboard

### 6. Quality & DevOps
- **SonarQube** — static analysis with JaCoCo coverage
- **Docker Compose** — single-command orchestration of 14 containers
- **JaCoCo** — 80%+ test coverage enforcement

## Security Architecture

```
Frontend → API Gateway (JWT validation) → X-Headers → Downstream Service
                                                           ↓
                                                  GatewayAuthenticationFilter
                                                  (reads X-Headers, sets SecurityContext)
                                                           ↓
                                                  @PreAuthorize("hasRole('ADMIN')")
```

1. User logs in → auth-service issues JWT (access + refresh tokens)
2. Frontend stores tokens, sends `Authorization: Bearer <JWT>` on every request
3. API Gateway's `JwtAuthenticationFilter` validates JWT, extracts claims, injects X-Headers
4. Downstream services' `GatewayAuthenticationFilter` reads X-Headers, builds `GatewayUserPrincipal`, sets Spring Security context
5. Method-level security via `@PreAuthorize` annotations

## Diagram

See [whole-project-architecture.mmd](whole-project-architecture.mmd) for the Mermaid source.
