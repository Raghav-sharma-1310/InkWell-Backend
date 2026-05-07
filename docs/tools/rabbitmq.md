# RabbitMQ Usage in InkWell

## Overview
RabbitMQ 3.13 is used as the asynchronous message broker for event-driven communication between microservices. It enables loose coupling — producers and consumers don't need to know about each other.

## Configuration
- **Image**: `rabbitmq:3.13-management`
- **AMQP Port**: 5672
- **Management UI Port**: 15672
- **Default Credentials**: guest/guest

## Exchange
All services share a single **DirectExchange**: `inkwell.exchange`

## Queues & Routing Keys

| Queue Name | Routing Key | Producer | Consumer | Purpose |
|---|---|---|---|---|
| `post-published-queue` | `post.published` | post-service | notification-service | Audit log when post is published |
| `category-post-deleted-queue` | `post.deleted` | post-service | category-service | Remove taxonomy mappings when post deleted |
| `comment-notification-queue` | `comment.new` | comment-service | notification-service | Notify post author of new comment |
| `reply-notification-queue` | `comment.reply` | comment-service | notification-service | Notify post author of reply |

## Message Format
All messages use **Jackson2JsonMessageConverter** for JSON serialization. Payloads are `Map<String, Object>`.

### Example: comment.new payload
```json
{
  "postId": "uuid-string",
  "postAuthorId": "uuid-string",
  "commentAuthorId": "uuid-string",
  "commentId": "uuid-string"
}
```

## Configuration Classes
- `com.inkwell.post.config.RabbitConfig` — declares exchange, `post-published-queue`, binding
- `com.inkwell.category.config.RabbitConfig` — declares exchange, `category-post-deleted-queue`, binding
- comment-service and notification-service also declare their queues via `@RabbitListener`

## Services NOT Using RabbitMQ
- api-gateway: No direct interaction
- auth-service: No direct interaction
- media-service: No direct interaction
- payment-service: No direct interaction
- discovery-service: No direct interaction
- admin-server: No direct interaction
