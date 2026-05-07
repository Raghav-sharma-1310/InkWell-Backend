# Media Service — Service Overview

## Purpose
Manages file uploads and media storage. Supports both **local filesystem** and **AWS S3** storage backends, switchable via configuration.

## Port: 8085 | Database: media_db

---

## Controllers

| Class | Base Path | Purpose |
|---|---|---|
| `MediaController` | `/api/media` | Upload, list, delete, serve media files |
| `ServiceInfoController` | `/` | Service info |

## Services

| Class | Purpose |
|---|---|
| `MediaService` | Core media logic: upload, list, delete, soft-delete |

## Storage Layer

| Class | Purpose |
|---|---|
| `StorageService` (interface) | Abstraction for file storage |
| `LocalStorageService` | Stores files on local filesystem (`uploads/media/`) |
| `S3StorageService` | Stores files in AWS S3 bucket |
| `StoredFile` | Record: path, publicUrl |

## Repository
`MediaRepository` — JPA repository for MediaFile entity

## Entity

| Class | Table | Key Fields |
|---|---|---|
| `MediaFile` | media_files | mediaId, uploaderId, filename, originalName, url, mimeType, sizeKb, altText, linkedPostId, deleted |

## DTOs
**Response**: MediaResponse

## Security Classes
`SecurityConfig`, `GatewayAuthenticationFilter`, `GatewayUserPrincipal`

## Config
`AppConfig` — application beans, storage mode selection

---

## APIs Exposed

### Public
| Method | Path | Description |
|---|---|---|
| GET | `/api/media/public/files/{filename}` | Serve a media file |

### Author
| Method | Path | Description |
|---|---|---|
| POST | `/api/media/author/upload` | Upload a file (multipart, max 10MB) |
| GET | `/api/media/author/mine` | List uploaded files |
| DELETE | `/api/media/author/{id}` | Soft-delete own file |

### Admin
| Method | Path | Description |
|---|---|---|
| GET | `/api/media/admin/all` | List all media files |
| DELETE | `/api/media/admin/{id}` | Hard-delete any file |

---

## Storage Configuration
```yaml
app:
  storage:
    mode: ${STORAGE_MODE:local}        # "local" or "s3"
    local-dir: uploads/media
    public-base-url: http://localhost:8080/api/media/public/files
    s3:
      bucket: ${AWS_S3_BUCKET}
      region: ${AWS_REGION:us-east-1}
      access-key: ${AWS_ACCESS_KEY_ID}
      secret-key: ${AWS_SECRET_ACCESS_KEY}
      public-base-url: ${AWS_S3_PUBLIC_BASE_URL}
```

## External Tools
- **MySQL**: media_db
- **AWS S3**: Optional cloud storage (when `STORAGE_MODE=s3`)
- **Eureka**: Service registration
- **Redis**: No direct interaction
- **RabbitMQ**: No direct interaction
- **Mailpit**: No direct interaction
