# Newsletter Service — Service Overview

## Purpose
Manages newsletter subscriptions with a **double opt-in** workflow, email campaigns, and subscriber management.

## Port: 8086 | Database: newsletter_db

---

## Controllers

| Class | Base Path | Purpose |
|---|---|---|
| `NewsletterController` | `/api/newsletter` | Subscribe, unsubscribe, verify, campaigns |
| `ServiceInfoController` | `/` | Service info |

**Assumption**: Controller methods inferred from standard REST patterns and route definitions in gateway.

## Services

| Class | Purpose |
|---|---|
| `NewsletterService` | Subscription lifecycle, campaign management, subscriber queries |
| `MailService` | Sends confirmation and campaign emails |
| `TemplateService` | Email template rendering |

## Repositories

| Class | Entity |
|---|---|
| `SubscriberRepository` | Subscriber |
| `CampaignRepository` | Campaign |

## Entities

| Class | Table | Key Fields |
|---|---|---|
| `Subscriber` | subscribers | subscriberId, email, userId, fullName, status (PENDING/CONFIRMED/UNSUBSCRIBED), token |
| `Campaign` | campaigns | campaignId, subject, content |

## Enums
`SubscriberStatus` (PENDING, CONFIRMED, UNSUBSCRIBED)

## DTOs
**Request**: SubscribeRequest, CampaignRequest  
**Response**: SubscriberResponse

## Security Classes
`SecurityConfig`, `GatewayAuthenticationFilter`, `GatewayUserPrincipal`

## Utility Classes
`SecurityUtils`

## Exception Classes
`GlobalExceptionHandler`, `MailDeliveryException`

---

## APIs Exposed

### Public
| Method | Path | Description |
|---|---|---|
| POST | `/api/newsletter/public/subscribe` | Subscribe to newsletter |
| GET | `/api/newsletter/verify?token=` | Verify subscription (double opt-in) |
| POST | `/api/newsletter/public/unsubscribe` | Unsubscribe |

### Admin
| Method | Path | Description |
|---|---|---|
| GET | `/api/newsletter/admin/subscribers` | List all subscribers |
| POST | `/api/newsletter/admin/campaigns` | Create and send campaign |
| GET | `/api/newsletter/admin/campaigns` | List campaigns |

---

## Double Opt-In Flow
```
1. User submits email → Subscriber created with status=PENDING, unique token generated
2. Confirmation email sent with verification link
3. User clicks link → GET /api/newsletter/verify?token=xxx
4. Subscriber status updated to CONFIRMED
```

## External Tools
- **MySQL**: newsletter_db
- **RabbitMQ**: Connected (config present) but primary usage is email-based
- **Mailpit/SMTP**: Sends confirmation and campaign emails
- **Eureka**: Service registration
