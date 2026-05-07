# Post Service (Blog Service) — Service Overview

## Purpose
The Post Service manages the **core blogging functionality**: creating, editing, publishing, scheduling, and managing blog posts. It also handles likes, bookmarks, follows, reading history, and author profiles.

## Port: 8082 | Database: post_db

---

## Controllers

| Class | Base Path | Purpose |
|---|---|---|
| `AuthorPostController` | `/api/posts/author` | Author: create, edit, delete own posts |
| `PublicPostController` | `/api/posts/public` | Public: browse published posts |
| `ExplorePostController` | `/api/posts/explore` | Public: trending, featured, latest posts |
| `ReaderPostController` | `/api/posts/reader` | Reader: like, bookmark, follows |
| `AdminPostController` | `/api/posts/admin` | Admin: feature/pin posts, moderation |
| `ReadingHistoryController` | `/api/reading-history` | Premium: reading history |
| `AuthorProfileController` | `/api/posts/authors` | Public: author profiles, follower counts |
| `InternalPostController` | `/api/posts/internal` | Internal: post metadata for comment-service |
| `ServiceInfoController` | `/` | Service info |

## Services

| Class | Purpose |
|---|---|
| `PostService` | Core post CRUD, publishing, scheduling, likes, views, search |
| `FollowBookmarkService` | Follow/unfollow authors, bookmark/unbookmark posts |
| `PostScheduler` | Scheduled task: publish posts when scheduledAt arrives |

## Repositories

| Class | Entity |
|---|---|
| `PostRepository` | Post |
| `PostLikeRepository` | PostLike |
| `BookmarkRepository` | Bookmark |
| `FollowRepository` | Follow |
| `PostHistoryRepository` | PostHistory |

## Entities

| Class | Table | Key Fields |
|---|---|---|
| `Post` | posts | postId, authorId, title, slug, content, status, visibility, viewCount, likesCount, categorySlug, tagSlugs, featured, pinned |
| `PostLike` | post_likes | postId, userId |
| `Bookmark` | bookmarks | userId, postId (unique constraint) |
| `Follow` | follows | followerId, followingId |
| `PostHistory` | post_history | userId, postId, readAt |

## Enums
`PostStatus` (DRAFT, PUBLISHED, SCHEDULED, ARCHIVED), `PostVisibility` (PUBLIC, PREMIUM)

## DTOs
**Request**: SavePostRequest  
**Response**: PostResponse, PostMetaResponse, LikeResponse, PageResponse

## Config Classes

| Class | Purpose |
|---|---|
| `CacheConfig` | Redis caching configuration |
| `RabbitConfig` | Declares `inkwell.exchange`, `post-published-queue` |
| `DataInitializer` | Seeds sample posts |
| `OpenApiConfig` | Swagger/OpenAPI config |
| `PostVisibilityConverter` | JPA converter for PostVisibility enum |

## Security Classes
`SecurityConfig`, `GatewayAuthenticationFilter`, `GatewayUserPrincipal`

## Utility Classes
`HtmlSanitizer` (sanitize HTML content), `ReadTimeUtil` (calculate reading time), `SecurityUtils` (extract current user), `SlugUtil` (generate URL slugs)

## Client Classes
`CategoryClient` — Feign client calling category-service for taxonomy sync

---

## APIs Exposed

### Public
| Method | Path | Description |
|---|---|---|
| GET | `/api/posts/public/` | List published posts (paginated) |
| GET | `/api/posts/public/{slug}` | Get post by slug |
| GET | `/api/posts/explore/trending` | Trending posts |
| GET | `/api/posts/explore/featured` | Featured posts |
| GET | `/api/posts/explore/latest` | Latest posts |
| GET | `/api/posts/authors/{id}/followers/count` | Follower count |

### Author
| Method | Path | Description |
|---|---|---|
| POST | `/api/posts/author/` | Create post |
| PUT | `/api/posts/author/{id}` | Update post |
| DELETE | `/api/posts/author/{id}` | Delete post |
| PATCH | `/api/posts/author/{id}/publish` | Publish post |
| GET | `/api/posts/author/mine` | List own posts |

### Reader
| Method | Path | Description |
|---|---|---|
| POST | `/api/posts/reader/{id}/like` | Like/unlike post |
| POST | `/api/posts/reader/{id}/bookmark` | Bookmark/unbookmark (Premium) |
| GET | `/api/posts/reader/bookmarks` | List bookmarks (Premium) |

### Admin
| Method | Path | Description |
|---|---|---|
| PATCH | `/api/posts/admin/{id}/feature` | Toggle featured |
| PATCH | `/api/posts/admin/{id}/pin` | Toggle pinned |

---

## External Tools
- **MySQL**: post_db schema
- **Redis**: Post data caching
- **RabbitMQ**: Publishes `post.published` and `post.deleted` events
