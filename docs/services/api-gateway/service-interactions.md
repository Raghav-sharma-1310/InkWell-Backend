# API Gateway — Service Interactions

## Upstream (receives from)
| Source | Protocol | Data | Purpose |
|---|---|---|---|
| React Frontend | HTTP/REST | All API requests | Single entry point for client |

## Downstream (sends to)
| Target Service | Protocol | Class | Data Exchanged | Purpose |
|---|---|---|---|---|
| auth-service | HTTP (lb://) | Route config | All `/api/auth/**` requests | Authentication & user management |
| post-service | HTTP (lb://) | Route config | All `/api/posts/**` requests | Post operations |
| comment-service | HTTP (lb://) | Route config | All `/api/comments/**` requests | Comment operations |
| category-service | HTTP (lb://) | Route config | All `/api/categories/**` requests | Category & tag operations |
| media-service | HTTP (lb://) | Route config | All `/api/media/**` requests | File upload/download |
| newsletter-service | HTTP (lb://) | Route config | All `/api/newsletter/**` requests | Newsletter operations |
| notification-service | HTTP (lb://) | Route config | All `/api/notifications/**` requests | Notification operations |
| payment-service | HTTP (lb://) | Route config | All `/api/payments/**` requests | Payment operations |

## Infrastructure
| Tool | Class | Purpose |
|---|---|---|
| Redis | `RateLimiterConfig` | Rate limiting key storage for newsletter routes |
| Eureka | Spring Cloud LoadBalancer | Service name → IP resolution |

## Headers Injected
The gateway adds these headers to every authenticated request before forwarding:
- `X-User-Id`: User's UUID
- `X-Username`: Username string
- `X-User-Role`: READER / AUTHOR / ADMIN
- `X-User-Email`: Email address
- `X-User-Subscription-Tier`: FREE / PRO
- `X-User-Subscription-Status`: ACTIVE / EXPIRED / null
