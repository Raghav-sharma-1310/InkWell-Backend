# Comment Service — Service Interactions

## Outbound Calls

| Target | Protocol | Class | Endpoint | Data Exchanged | Purpose |
|---|---|---|---|---|---|
| post-service | REST (Feign) | `PostClient` | `GET /api/posts/internal/{postId}/meta` | Returns `PostMetaResponse` (postId, authorId, title) | Validate post exists + get author for notifications |
| RabbitMQ | AMQP | `RabbitTemplate` | Exchange: `inkwell.exchange`, Key: `comment.new` | `{postId, postAuthorId, commentAuthorId, commentId}` | Trigger notification for new comment |
| RabbitMQ | AMQP | `RabbitTemplate` | Exchange: `inkwell.exchange`, Key: `comment.reply` | `{postId, postAuthorId, commentAuthorId, commentId}` | Trigger notification for reply |

## Inbound Calls

| Source | Protocol | Endpoint | Purpose |
|---|---|---|---|
| API Gateway | HTTP | All `/api/comments/**` routes | Client requests |

## Tool Involvement

| Tool | Usage |
|---|---|
| MySQL (comment_db) | Primary data store |
| RabbitMQ | Event publishing (comment.new, comment.reply) |
| Eureka | Service registration + post-service Feign resolution |
| Redis | No direct interaction |
| Mailpit | No direct interaction |
