# Service Interaction Diagram

## Overview

This document describes how InkWell microservices communicate with each other. There are two primary communication patterns:

1. **Synchronous REST (OpenFeign)** — service-to-service HTTP calls via Eureka-resolved names
2. **Asynchronous Events (RabbitMQ)** — fire-and-forget event publishing with durable queues

---

## Synchronous Interactions (Feign Clients)

| Source Service | Target Service | Feign Client Class | Endpoint Called | Purpose |
|---|---|---|---|---|
| post-service | category-service | `CategoryClient` | `POST /api/categories/internal/posts/{postId}/taxonomy` | Sync category & tag mappings when a post is created/updated |
| comment-service | post-service | `PostClient` | `GET /api/posts/internal/{postId}/meta` | Validate post existence and fetch author info before allowing a comment |
| notification-service | auth-service | `AuthClient` | `GET /api/auth/internal/users/{userId}` | Fetch user email for sending notification emails |
| notification-service | auth-service | `AuthClient` | `GET /api/auth/public/search?query=` | List all users for admin broadcast delivery |

All Feign clients use:
- **Eureka** for service name resolution (`@FeignClient(name = "service-name")`)
- **Resilience4j** circuit breakers (sliding window, 50% failure threshold)
- **Load balancer** retry with caching (35s TTL)

---

## Asynchronous Interactions (RabbitMQ)

All events flow through a shared **DirectExchange** named `inkwell.exchange`.

| Producer | Routing Key | Consumer Service | Queue Name | Trigger | Effect |
|---|---|---|---|---|---|
| post-service | `post.published` | notification-service | `post-published-notification-queue` | Post status → PUBLISHED | Audit log entry created |
| post-service | `post.deleted` | category-service | `category-post-deleted-queue` | Post permanently deleted | Category/tag mappings removed |
| comment-service | `comment.new` | notification-service | `comment-notification-queue` | New comment created | In-app + email notification to post author |
| comment-service | `comment.reply` | notification-service | `reply-notification-queue` | Reply to a comment | In-app + email notification to post author |
| notification-service | `admin.broadcast` | — | — | Admin sends broadcast | Published to exchange (no consumer currently) |

---

## Request Flow Through Gateway

```
Frontend (5173) → API Gateway (8080) → Downstream Service
                        |
                  JWT Validation
                  Role Check
                  X-Headers Injection
```

The API Gateway adds these headers to every authenticated request:
- `X-User-Id` — UUID
- `X-Username` — string
- `X-User-Role` — READER / AUTHOR / ADMIN
- `X-User-Email` — email string
- `X-User-Subscription-Tier` — FREE / PRO
- `X-User-Subscription-Status` — ACTIVE / EXPIRED / null

---

## Diagram

See [service-interaction-diagram.mmd](service-interaction-diagram.mmd) for the Mermaid source.
