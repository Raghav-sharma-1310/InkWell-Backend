# Notification Service — Service Overview

## Purpose
Manages in-app notifications, email notifications, audit logging, and admin broadcast delivery. It is the primary **event consumer** in the platform, listening to RabbitMQ events from post-service and comment-service.

## Port: 8087 | Database: notification_db

---

## Controllers

| Class | Base Path | Purpose |
|---|---|---|
| `NotificationController` | `/api/notifications` | User: list, mark read, delete notifications |
| `AdminNotificationController` | `/api/admin/notifications` | Admin: broadcasts, audit logs, delete |
| `ServiceInfoController` | `/` | Service info |

## Services

| Class | Purpose |
|---|---|
| `NotificationService` | Core notification logic, RabbitMQ listeners, broadcast, audit |
| `MailService` | Sends HTML notification emails via SMTP |

## Repositories

| Class | Entity |
|---|---|
| `NotificationRepository` | Notification |
| `AuditLogRepository` | AuditLog |

## Entities

| Class | Table | Key Fields |
|---|---|---|
| `Notification` | notifications | notificationId, recipientId, actorId, type, title, message, relatedId, relatedType, read |
| `AuditLog` | audit_logs | auditId, actorId, action, source, details |

## Enums
`NotificationType` (NEW_COMMENT, COMMENT_REPLY, ADMIN_BROADCAST, ...)

## DTOs
**Request**: BroadcastRequest  
**Response**: NotificationResponse, AuditLogResponse, UserResponse

## Client Classes
`AuthClient` — Feign client calling auth-service for user info and user listing

## Security Classes
`SecurityConfig`, `GatewayAuthenticationFilter`, `GatewayUserPrincipal`

## Config
`AppConfig` — application beans

---

## APIs Exposed

### Authenticated
| Method | Path | Description |
|---|---|---|
| GET | `/api/notifications/mine` | List user's notifications |
| GET | `/api/notifications/unread-count` | Get unread count |
| PATCH | `/api/notifications/{id}/read` | Mark single notification as read |
| PATCH | `/api/notifications/read-all` | Mark all as read |
| DELETE | `/api/notifications/read` | Delete all read notifications |

### Admin Only
| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/notifications/broadcast` | Send broadcast to all users |
| GET | `/api/admin/notifications/audits` | Get audit logs |
| DELETE | `/api/admin/notifications/{id}` | Delete notification |
| DELETE | `/api/admin/notifications/broadcast/{broadcastId}` | Delete broadcast |

---

## RabbitMQ Listeners

| Queue | Method | Trigger | Action |
|---|---|---|---|
| `comment-notification-queue` | `onComment()` | New comment created | Create notification + send email to post author |
| `reply-notification-queue` | `onReply()` | Reply to comment | Create notification + send email to post author |
| `post-published-notification-queue` | `onPostPublished()` | Post published | Create audit log entry |

## External Tools
- **MySQL**: notification_db
- **RabbitMQ**: Consumes events from post/comment services; publishes `admin.broadcast`
- **Mailpit/SMTP**: Sends notification emails
- **Eureka**: Service registration + auth-service Feign resolution
