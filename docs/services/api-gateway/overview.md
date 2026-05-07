# API Gateway — Service Overview

## Purpose
The API Gateway is the **single entry point** for all client requests. It handles JWT validation, role-based authorization, request routing, rate limiting, circuit breaking, and Swagger aggregation.

## Responsibilities
- Route HTTP requests to downstream microservices via Eureka-resolved names
- Validate JWT tokens and extract user claims
- Enforce role-based access (ADMIN, AUTHOR, PREMIUM paths)
- Inject `X-User-*` headers into downstream requests
- Rate limit newsletter endpoints via Redis
- Circuit break failing downstream services
- Aggregate Swagger documentation from all services
- Handle CORS for the React frontend

## Tech Stack
- Spring Cloud Gateway (WebFlux/Reactive)
- Spring Cloud Netflix Eureka Client
- Spring Data Redis
- Resilience4j (Circuit Breaker + Time Limiter)
- SpringDoc OpenAPI

---

## Controllers
| Class | Path | Purpose |
|---|---|---|
| `ApiGatewayInfoController` | `/` | Service info endpoint |
| `GatewayFallbackController` | `/fallback` | Circuit breaker fallback |

## Security Classes
| Class | Purpose |
|---|---|
| `JwtAuthenticationFilter` | Global filter — validates JWT, enforces roles, injects X-Headers |
| `JwtService` | Parses JWT tokens using the shared secret |

## Config Classes
| Class | Purpose |
|---|---|
| `RateLimiterConfig` | Defines `userOrIpKeyResolver` bean for Redis rate limiting |
| `SwaggerConfig` | Aggregates OpenAPI docs from all downstream services |

---

## APIs Exposed (Route Definitions)

| Route ID | Path Predicate | Downstream Service |
|---|---|---|
| auth-service | `/api/auth/**`, `/api/author-request/**`, `/api/admin/author-request/**`, `/api/feedback/**`, `/api/admin/feedback/**` | auth-service |
| auth-oauth | `/oauth2/**`, `/login/**` | auth-service |
| post-service | `/api/posts/**`, `/api/reading-history/**` | post-service |
| comment-service | `/api/comments/**` | comment-service |
| category-service | `/api/categories/**` | category-service |
| media-service | `/api/media/**` | media-service |
| newsletter-service | `/api/newsletter/**` | newsletter-service (rate limited) |
| notification-service | `/api/notifications/**`, `/api/admin/notifications/**` | notification-service |
| payment-service | `/api/payments/**` | payment-service |

## Path Security Rules

### Public (no token required)
`/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`, `/api/posts/public/**`, `/api/comments/public/**`, `/api/categories/public/**`, `/api/newsletter/public/**`, `/api/media/public/**`, `/actuator/**`, Swagger paths

### Admin Only
`/api/auth/admin/**`, `/api/posts/admin/**`, `/api/comments/admin/**`, `/api/categories/admin/**`, `/api/media/admin/**`, `/api/newsletter/admin/**`, `/api/notifications/admin/**`

### Author Only
`/api/posts/author/**`, `/api/comments/author/**`, `/api/media/author/**`

### Premium Only
`/api/posts/reader/bookmarks`, `/api/reading-history/**`

---

## Database
No database. Stateless.

## External Tools
- **Redis**: Rate limiting key storage
- **Eureka**: Service name resolution for `lb://` routing

## Full Request Flow
```
1. Client sends request to :8080
2. JwtAuthenticationFilter intercepts
3. If OPTIONS → pass through
4. If public path and no token → pass through
5. If token present → parse JWT via JwtService
6. Check role authorization (admin/author/premium)
7. Build mutated request with X-User-* headers
8. Route to downstream service via Eureka
9. If downstream fails → CircuitBreaker → /fallback
```
