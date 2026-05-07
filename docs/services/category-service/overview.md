# Category Service — Service Overview

## Purpose
Manages **categories and tags** (taxonomy) for blog posts. Provides public browsing, admin CRUD, and internal taxonomy synchronization for the post-service.

## Port: 8083 | Database: category_db

---

## Controllers

| Class | Base Path | Purpose |
|---|---|---|
| `PublicCategoryController` | `/api/categories/public` | Public: browse categories and tags |
| `AdminCategoryController` | `/api/categories/admin` | Admin: CRUD categories and tags |
| `InternalCategoryController` | `/api/categories/internal` | Internal: taxonomy sync from post-service |
| `ServiceInfoController` | `/` | Service info |

## Services

| Class | Purpose |
|---|---|
| `CategoryService` | Category & tag CRUD, taxonomy sync, post count management |

## Repositories

| Class | Entity |
|---|---|
| `CategoryRepository` | Category |
| `TagRepository` | Tag |
| `PostCategoryMappingRepository` | PostCategoryMapping |
| `PostTagMappingRepository` | PostTagMapping |

## Entities

| Class | Table | Key Fields |
|---|---|---|
| `Category` | categories | categoryId, name, slug, description, parentCategoryId, postCount |
| `Tag` | tags | tagId, name, slug, postCount |
| `PostCategoryMapping` | post_category_mappings | postId, categoryId |
| `PostTagMapping` | post_tag_mappings | postId, tagId |

## DTOs
**Request**: CategoryRequest, TagRequest, TaxonomySyncRequest  
**Response**: CategoryResponse, TagResponse

## Config Classes
| Class | Purpose |
|---|---|
| `RabbitConfig` | Declares `inkwell.exchange`, `category-post-deleted-queue` |
| `DataInitializer` | Seeds default categories |
| `OpenApiConfig` | Swagger/OpenAPI config |

## Security Classes
`SecurityConfig`, `GatewayAuthenticationFilter`, `GatewayUserPrincipal`

## Utility Classes
`SlugUtil` — URL slug generation

---

## APIs Exposed

### Public
| Method | Path | Description |
|---|---|---|
| GET | `/api/categories/public/` | List all categories |
| GET | `/api/categories/public/{slug}` | Get category by slug |
| GET | `/api/categories/public/tags` | List all tags |

### Admin
| Method | Path | Description |
|---|---|---|
| POST | `/api/categories/admin/` | Create category |
| PUT | `/api/categories/admin/{id}` | Update category |
| DELETE | `/api/categories/admin/{id}` | Delete category |
| POST | `/api/categories/admin/tags` | Create tag |
| DELETE | `/api/categories/admin/tags/{id}` | Delete tag |

### Internal
| Method | Path | Description |
|---|---|---|
| POST | `/api/categories/internal/posts/{postId}/taxonomy` | Sync category & tag mappings for a post |

---

## RabbitMQ Consumer
Listens to `category-post-deleted-queue` (routing key: `post.deleted`) to clean up post-category and post-tag mappings when a post is deleted.

## External Tools
- **MySQL**: category_db
- **RabbitMQ**: Consumes `post.deleted` events
- **Eureka**: Service registration
- **Redis**: No direct interaction
- **Mailpit**: No direct interaction
