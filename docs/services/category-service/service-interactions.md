# Category Service — Service Interactions

## Inbound Calls
| Source | Protocol | Endpoint | Data Received | Purpose |
|---|---|---|---|---|
| post-service | REST (Feign) | `POST /api/categories/internal/posts/{postId}/taxonomy` | `TaxonomySyncRequest` (categorySlug, tagSlugs) | Sync taxonomy mappings |
| RabbitMQ | AMQP | `category-post-deleted-queue` (key: `post.deleted`) | `{postId}` | Clean up mappings on post deletion |
| API Gateway | HTTP | `/api/categories/**` | Client requests | Public/admin CRUD |

## Outbound Calls
No outbound Feign calls to other services.

## Tool Involvement
| Tool | Usage |
|---|---|
| MySQL (category_db) | Primary data store |
| RabbitMQ | Consumes `post.deleted` events |
| Eureka | Service registration |
| Redis | No direct interaction |
| Mailpit | No direct interaction |
