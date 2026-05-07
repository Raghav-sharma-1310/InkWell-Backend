# User Service — Service Interactions

> User management is part of auth-service. See [auth-service/service-interactions.md](../auth-service/service-interactions.md) for complete details.

## Inbound (User-Related)
| Source | Protocol | Endpoint | Purpose |
|---|---|---|---|
| API Gateway | HTTP | `/api/auth/me`, `/api/auth/admin/users/**` | Client requests |
| notification-service | Feign | `GET /api/auth/internal/users/{id}` | Fetch user email |
| notification-service | Feign | `GET /api/auth/public/search` | List all users for broadcast |

## Outbound
| Target | Protocol | Purpose |
|---|---|---|
| MySQL (auth_db) | JDBC | User entity persistence |
| Mailpit/SMTP | SMTP | Welcome emails on registration |

## Tool Involvement
| Tool | Usage |
|---|---|
| MySQL | Users table in auth_db |
| Eureka | Service registration |
| Redis | No direct interaction |
| RabbitMQ | No direct interaction |
