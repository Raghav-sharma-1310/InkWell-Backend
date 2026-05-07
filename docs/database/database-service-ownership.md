# Database Service Ownership

## Overview

InkWell follows a strict **database-per-service** pattern. Each microservice has exclusive ownership of its MySQL schema. No service directly accesses another service's database. Cross-service data access is achieved via REST APIs (Feign) or RabbitMQ events.

---

## Ownership Map

| Schema | Owner Service | Port | Tables |
|---|---|---|---|
| **auth_db** | auth-service | 8081 | users, refresh_tokens, email_verification_tokens, password_otps, payment_orders, audit_logs, author_requests, feedback_reports, feedback_messages |
| **post_db** | post-service | 8082 | posts, post_tags, post_likes, bookmarks, follows, post_history |
| **category_db** | category-service | 8083 | categories, tags, post_category_mappings, post_tag_mappings |
| **comment_db** | comment-service | 8084 | comments, comment_likes |
| **media_db** | media-service | 8085 | media_files |
| **newsletter_db** | newsletter-service | 8086 | subscribers, campaigns |
| **notification_db** | notification-service | 8087 | notifications, audit_logs |

---

## Services WITHOUT Databases

| Service | Reason |
|---|---|
| api-gateway | Stateless edge router; uses Redis for rate limiting only |
| discovery-service | In-memory Eureka registry |
| admin-server | Dashboard that reads actuator endpoints |
| payment-service | Delegates to Razorpay; payment records stored in auth-service |
| frontend-web | Client-side SPA; uses localStorage for session |

---

## Cross-Service Data Access Patterns

| Data Needed By | Data Owner | Access Method |
|---|---|---|
| comment-service needs post author | post-service | Feign: `GET /api/posts/internal/{id}/meta` |
| notification-service needs user email | auth-service | Feign: `GET /api/auth/internal/users/{id}` |
| notification-service needs all users | auth-service | Feign: `GET /api/auth/public/search?query=` |
| category-service needs post deletion event | post-service | RabbitMQ: `post.deleted` → `category-post-deleted-queue` |
| notification-service needs publish event | post-service | RabbitMQ: `post.published` → `post-published-notification-queue` |
| notification-service needs comment event | comment-service | RabbitMQ: `comment.new` / `comment.reply` → queues |
| post-service needs taxonomy sync | category-service | Feign: `POST /api/categories/internal/posts/{id}/taxonomy` |

---

## Schema Creation

All schemas are auto-created by the JDBC connection URL parameter: `createDatabaseIfNotExist=true`

Hibernate `ddl-auto: update` handles table creation and schema evolution automatically.
