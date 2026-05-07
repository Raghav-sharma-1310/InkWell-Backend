# Post Service (Blog Service) — Service Interactions

## Outbound Calls (this service → others)

| Target | Protocol | Class | Endpoint | Data Exchanged | Purpose |
|---|---|---|---|---|---|
| category-service | REST (Feign) | `CategoryClient` | `POST /api/categories/internal/posts/{postId}/taxonomy` | `TaxonomySyncRequest` (categorySlug, tagSlugs) | Sync taxonomy when post created/updated |
| RabbitMQ | AMQP | `RabbitTemplate` | Exchange: `inkwell.exchange`, Key: `post.published` | `{postId, authorId, title}` | Notify notification-service of published post |
| RabbitMQ | AMQP | `RabbitTemplate` | Exchange: `inkwell.exchange`, Key: `post.deleted` | `{postId}` | Notify category-service to clean up mappings |
| Redis | TCP | Spring Cache | — | Post data | Cache frequently-read posts |

## Inbound Calls (others → this service)

| Source | Protocol | Endpoint | Data Returned | Purpose |
|---|---|---|---|---|
| comment-service | REST (Feign) | `GET /api/posts/internal/{postId}/meta` | `ApiResponse<PostMetaResponse>` (postId, authorId, title) | Validate post exists before creating comment |
| API Gateway | HTTP | All `/api/posts/**` routes | Various | Client requests |

## Tool Involvement

| Tool | Usage |
|---|---|
| MySQL (post_db) | Primary data store |
| Redis | Post data caching |
| RabbitMQ | Event publishing (post.published, post.deleted) |
| Eureka | Service registration + discovery of category-service |
