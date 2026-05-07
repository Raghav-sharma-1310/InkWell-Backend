# Notification Service — Service Interactions

## Outbound Calls

| Target | Protocol | Class | Endpoint | Data Exchanged | Purpose |
|---|---|---|---|---|---|
| auth-service | REST (Feign) | `AuthClient` | `GET /api/auth/internal/users/{userId}` | Returns `UserResponse` (email, name) | Get recipient email for notification emails |
| auth-service | REST (Feign) | `AuthClient` | `GET /api/auth/public/search?query=` | Returns `List<UserResponse>` | List all users for admin broadcast delivery |
| RabbitMQ | AMQP | `RabbitTemplate` | Exchange: `inkwell.exchange`, Key: `admin.broadcast` | `{title, message, actorId}` | Publish broadcast event |
| Mailpit/SMTP | SMTP | `MailService` | — | HTML emails | Send notification emails |

## Inbound Calls

| Source | Protocol | Endpoint/Queue | Purpose |
|---|---|---|---|
| API Gateway | HTTP | All `/api/notifications/**` routes | Client requests |
| RabbitMQ | AMQP | `comment-notification-queue` | New comment events from comment-service |
| RabbitMQ | AMQP | `reply-notification-queue` | Reply events from comment-service |
| RabbitMQ | AMQP | `post-published-notification-queue` | Post published events from post-service |

## Tool Involvement

| Tool | Usage |
|---|---|
| MySQL (notification_db) | Primary data store (notifications, audit_logs) |
| RabbitMQ | Event consumer (3 queues) + event publisher (admin.broadcast) |
| Mailpit/SMTP | Email delivery for notifications |
| Eureka | Service registration + auth-service Feign resolution |
| Redis | No direct interaction |
