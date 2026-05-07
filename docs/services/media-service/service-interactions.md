# Media Service — Service Interactions

## Outbound Calls
| Target | Protocol | Purpose |
|---|---|---|
| Local Filesystem | File I/O | Store/read files when STORAGE_MODE=local |
| AWS S3 | HTTPS (SDK) | Store/read files when STORAGE_MODE=s3 |

## Inbound Calls
| Source | Protocol | Endpoint | Purpose |
|---|---|---|---|
| API Gateway | HTTP | `/api/media/**` | Client requests |

## Tool Involvement
| Tool | Usage |
|---|---|
| MySQL (media_db) | Media metadata storage |
| AWS S3 | Cloud file storage (optional) |
| Eureka | Service registration |
| Redis | No direct interaction |
| RabbitMQ | No direct interaction |
| Mailpit | No direct interaction |
