# Comment Service — Service Overview

## Purpose
Manages comments on blog posts, including threaded replies, comment likes, and admin moderation.

## Port: 8084 | Database: comment_db

---

## Controllers

| Class | Base Path | Purpose |
|---|---|---|
| `CommentController` | `/api/comments` | CRUD: create, read, update, delete comments; like/unlike; moderation |
| `ServiceInfoController` | `/` | Service info |

## Services

| Class | Purpose |
|---|---|
| `CommentService` | Core comment logic: create, update, delete, like, thread replies, admin moderation |

## Repositories

| Class | Entity |
|---|---|
| `CommentRepository` | Comment |
| `CommentLikeRepository` | CommentLike |

## Entities

| Class | Table | Key Fields |
|---|---|---|
| `Comment` | comments | commentId, postId, authorId, authorName, parentCommentId (for threads), content, likesCount, status |
| `CommentLike` | comment_likes | commentId, userId |

## Enums
`CommentStatus` (VISIBLE, HIDDEN, FLAGGED)

## DTOs
**Request**: CommentRequest, UpdateCommentRequest  
**Response**: CommentResponse, LikeResponse, PostMetaResponse

## Client Classes
`PostClient` — Feign client calling post-service to validate post existence and get author info

## Security Classes
`SecurityConfig`, `GatewayAuthenticationFilter`, `GatewayUserPrincipal`

## Utility Classes
`HtmlSanitizer`, `SecurityUtils`

## Config
`AppConfig` — application-level beans

---

## APIs Exposed

### Public
| Method | Path | Description |
|---|---|---|
| GET | `/api/comments/public/posts/{postId}` | List comments for a post |

### Authenticated
| Method | Path | Description |
|---|---|---|
| POST | `/api/comments/` | Create comment |
| PUT | `/api/comments/{id}` | Update own comment |
| DELETE | `/api/comments/{id}` | Delete own comment |
| POST | `/api/comments/{id}/like` | Like/unlike comment |

### Admin
| Method | Path | Description |
|---|---|---|
| PATCH | `/api/comments/admin/{id}/hide` | Hide comment |
| DELETE | `/api/comments/admin/{id}` | Delete any comment |

---

## External Tools
- **MySQL**: comment_db
- **RabbitMQ**: Publishes `comment.new` and `comment.reply` events
- **Eureka**: Service registration + post-service Feign resolution
