# Newsletter Service — Service Interactions

## Outbound Calls
| Target | Protocol | Class | Purpose |
|---|---|---|---|
| Mailpit/SMTP | SMTP | `MailService` | Send confirmation and campaign emails |

## Inbound Calls
| Source | Protocol | Endpoint | Purpose |
|---|---|---|---|
| API Gateway | HTTP | `/api/newsletter/**` | Client requests |

## Tool Involvement
| Tool | Usage |
|---|---|
| MySQL (newsletter_db) | Primary data store (subscribers, campaigns) |
| RabbitMQ | Connected but no active listeners found in codebase |
| Mailpit/SMTP | Email delivery for confirmations and campaigns |
| Eureka | Service registration |
| Redis | No direct interaction |
