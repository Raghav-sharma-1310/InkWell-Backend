# Database Design

## Overview

InkWell uses **MySQL 8.4** with a **database-per-service** pattern. Each microservice owns its own schema and manages its tables via JPA/Hibernate (`ddl-auto: update`). Services never share database connections or access another service's tables directly.

---

## auth_db (Auth Service)

### users
| Column | Type | Constraints | Description |
|---|---|---|---|
| user_id | UUID (PK) | NOT NULL, auto-generated | Primary key |
| username | VARCHAR(80) | UNIQUE, NOT NULL | Login username |
| email | VARCHAR(120) | UNIQUE, NOT NULL | Email address |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt-encoded password |
| full_name | VARCHAR(120) | NOT NULL | Display name |
| role | ENUM(READER, AUTHOR, ADMIN) | NOT NULL | User role |
| bio | VARCHAR(1000) | NULLABLE | Profile bio |
| avatar_url | VARCHAR(500) | NULLABLE | Profile picture URL |
| phone_number | VARCHAR(20) | NULLABLE | Phone number |
| provider | ENUM(LOCAL, GOOGLE, GITHUB) | NOT NULL | Authentication provider |
| is_active | BOOLEAN | NOT NULL | Account active status |
| subscription_tier | ENUM(FREE, PRO) | NOT NULL, DEFAULT FREE | Subscription level |
| subscription_status | ENUM(ACTIVE, EXPIRED, ...) | NULLABLE | Subscription status |
| subscription_end_date | DATETIME | NULLABLE | When subscription expires |
| created_at | DATETIME | NOT NULL | Account creation timestamp |
| updated_at | DATETIME | NOT NULL | Last update timestamp |

### refresh_tokens
| Column | Type | Constraints |
|---|---|---|
| id | UUID (PK) | NOT NULL |
| token | VARCHAR(255) | UNIQUE, NOT NULL |
| user_id | UUID (FK → users) | NOT NULL |
| expiry_date | DATETIME | NOT NULL |
| revoked | BOOLEAN | NOT NULL |

### email_verification_tokens
| Column | Type | Constraints |
|---|---|---|
| id | UUID (PK) | NOT NULL |
| token | VARCHAR(255) | UNIQUE, NOT NULL |
| user_id | UUID (FK → users) | NOT NULL |
| used | BOOLEAN | NOT NULL |
| created_at | DATETIME | NOT NULL |

### password_otps
| Column | Type | Constraints |
|---|---|---|
| id | UUID (PK) | NOT NULL |
| email | VARCHAR(120) | NOT NULL |
| otp | VARCHAR(10) | NOT NULL |
| expiry_time | DATETIME | NOT NULL |
| used | BOOLEAN | NOT NULL |

### payment_orders
| Column | Type | Constraints |
|---|---|---|
| order_id | UUID (PK) | NOT NULL |
| user_id | UUID (FK → users) | NOT NULL |
| gateway_order_id | VARCHAR(255) | UNIQUE |
| gateway_payment_id | VARCHAR(255) | NULLABLE |
| amount | DECIMAL | NOT NULL |
| currency | VARCHAR(10) | NOT NULL |
| status | ENUM(CREATED, PAID, FAILED) | NOT NULL |
| provider | ENUM(RAZORPAY) | NOT NULL |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

### audit_logs (auth)
| Column | Type | Constraints |
|---|---|---|
| audit_id | UUID (PK) | NOT NULL |
| user_id | UUID | NULLABLE |
| actor | VARCHAR(255) | NOT NULL |
| action | VARCHAR(255) | NOT NULL |
| entity_type | VARCHAR(100) | NOT NULL |
| entity_id | VARCHAR(255) | NOT NULL |
| details | VARCHAR(1500) | NOT NULL |
| created_at | DATETIME | NOT NULL |

### author_requests
| Column | Type | Constraints |
|---|---|---|
| request_id | UUID (PK) | NOT NULL |
| user_id | UUID (FK → users) | NOT NULL |
| status | ENUM(PENDING, APPROVED, REJECTED) | NOT NULL |
| reason | TEXT | NULLABLE |
| created_at | DATETIME | NOT NULL |
| reviewed_at | DATETIME | NULLABLE |

### feedback_reports
| Column | Type | Constraints |
|---|---|---|
| report_id | UUID (PK) | NOT NULL |
| reporter_id | UUID | NOT NULL |
| subject | VARCHAR(255) | NOT NULL |
| description | TEXT | NOT NULL |
| status | ENUM(OPEN, IN_PROGRESS, RESOLVED, CLOSED) | NOT NULL |
| priority | VARCHAR(20) | NULLABLE |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

### feedback_messages
| Column | Type | Constraints |
|---|---|---|
| message_id | UUID (PK) | NOT NULL |
| report_id | UUID (FK → feedback_reports) | NOT NULL |
| sender_id | UUID | NOT NULL |
| content | TEXT | NOT NULL |
| created_at | DATETIME | NOT NULL |

---

## post_db (Post Service)

### posts
| Column | Type | Constraints |
|---|---|---|
| post_id | UUID (PK) | NOT NULL |
| author_id | UUID | NOT NULL |
| title | VARCHAR(180) | NOT NULL |
| slug | VARCHAR(220) | UNIQUE, NOT NULL |
| content | LONGTEXT | NOT NULL |
| excerpt | VARCHAR(500) | NULLABLE |
| featured_image_url | VARCHAR(500) | NULLABLE |
| status | ENUM(DRAFT, PUBLISHED, SCHEDULED, ARCHIVED) | NOT NULL |
| visibility | ENUM(PUBLIC, PREMIUM) | NOT NULL, DEFAULT PUBLIC |
| read_time_min | INT | NOT NULL |
| view_count | BIGINT | NOT NULL, DEFAULT 0 |
| likes_count | BIGINT | NOT NULL, DEFAULT 0 |
| category_slug | VARCHAR(120) | NULLABLE |
| is_featured | BOOLEAN | NOT NULL |
| pinned | BOOLEAN | NOT NULL |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |
| published_at | DATETIME | NULLABLE |
| scheduled_at | DATETIME | NULLABLE |

### post_tags (ElementCollection)
| Column | Type | Constraints |
|---|---|---|
| post_id | UUID (FK → posts) | NOT NULL |
| tag_slug | VARCHAR(255) | NOT NULL |

### post_likes
| Column | Type | Constraints |
|---|---|---|
| like_id | UUID (PK) | NOT NULL |
| post_id | UUID | NOT NULL |
| user_id | UUID | NOT NULL |
| created_at | DATETIME | NOT NULL |

### bookmarks
| Column | Type | Constraints |
|---|---|---|
| bookmark_id | UUID (PK) | NOT NULL |
| user_id | UUID | NOT NULL |
| post_id | UUID | NOT NULL |
| created_at | DATETIME | NOT NULL |
| UNIQUE(user_id, post_id) | | |

### follows
| Column | Type | Constraints |
|---|---|---|
| follow_id | UUID (PK) | NOT NULL |
| follower_id | UUID | NOT NULL |
| following_id | UUID | NOT NULL |
| created_at | DATETIME | NOT NULL |

### post_history (reading history)
| Column | Type | Constraints |
|---|---|---|
| history_id | UUID (PK) | NOT NULL |
| user_id | UUID | NOT NULL |
| post_id | UUID | NOT NULL |
| read_at | DATETIME | NOT NULL |

---

## comment_db (Comment Service)

### comments
| Column | Type | Constraints |
|---|---|---|
| comment_id | UUID (PK) | NOT NULL |
| post_id | UUID | NOT NULL |
| author_id | UUID | NOT NULL |
| author_name | VARCHAR(100) | NULLABLE |
| parent_comment_id | UUID | NULLABLE (self-referencing for threads) |
| content | TEXT | NOT NULL |
| likes_count | BIGINT | NOT NULL, DEFAULT 0 |
| status | ENUM(VISIBLE, HIDDEN, FLAGGED) | NOT NULL |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

### comment_likes
| Column | Type | Constraints |
|---|---|---|
| like_id | UUID (PK) | NOT NULL |
| comment_id | UUID | NOT NULL |
| user_id | UUID | NOT NULL |
| created_at | DATETIME | NOT NULL |

---

## category_db (Category Service)

### categories
| Column | Type | Constraints |
|---|---|---|
| category_id | UUID (PK) | NOT NULL |
| name | VARCHAR(120) | UNIQUE, NOT NULL |
| slug | VARCHAR(140) | UNIQUE, NOT NULL |
| description | VARCHAR(500) | NULLABLE |
| parent_category_id | UUID | NULLABLE (self-referencing) |
| post_count | BIGINT | NOT NULL, DEFAULT 0 |
| created_at | DATETIME | NOT NULL |

### tags
| Column | Type | Constraints |
|---|---|---|
| tag_id | UUID (PK) | NOT NULL |
| name | VARCHAR(120) | UNIQUE, NOT NULL |
| slug | VARCHAR(140) | UNIQUE, NOT NULL |
| post_count | BIGINT | NOT NULL, DEFAULT 0 |

### post_category_mappings
| Column | Type | Constraints |
|---|---|---|
| id | UUID (PK) | NOT NULL |
| post_id | UUID | NOT NULL |
| category_id | UUID (FK → categories) | NOT NULL |

### post_tag_mappings
| Column | Type | Constraints |
|---|---|---|
| id | UUID (PK) | NOT NULL |
| post_id | UUID | NOT NULL |
| tag_id | UUID (FK → tags) | NOT NULL |

---

## media_db (Media Service)

### media_files
| Column | Type | Constraints |
|---|---|---|
| media_id | UUID (PK) | NOT NULL |
| uploader_id | UUID | NOT NULL |
| filename | VARCHAR(180) | NOT NULL |
| original_name | VARCHAR(255) | NOT NULL |
| url | VARCHAR(500) | NOT NULL |
| mime_type | VARCHAR(120) | NOT NULL |
| size_kb | BIGINT | NOT NULL |
| alt_text | VARCHAR(255) | NULLABLE |
| linked_post_id | UUID | NULLABLE |
| uploaded_at | DATETIME | NOT NULL |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false |

---

## newsletter_db (Newsletter Service)

### subscribers
| Column | Type | Constraints |
|---|---|---|
| subscriber_id | UUID (PK) | NOT NULL |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| user_id | UUID | NULLABLE |
| full_name | VARCHAR(255) | NULLABLE |
| status | ENUM(PENDING, CONFIRMED, UNSUBSCRIBED) | NOT NULL |
| subscribed_at | DATETIME | NOT NULL |
| unsubscribed_at | DATETIME | NULLABLE |
| token | VARCHAR(255) | UNIQUE, NOT NULL |
| preferences | VARCHAR(255) | NULLABLE |

### campaigns
| Column | Type | Constraints |
|---|---|---|
| campaign_id | UUID (PK) | NOT NULL |
| subject | VARCHAR(255) | NOT NULL |
| content | TEXT | NOT NULL |
| created_at | DATETIME | NOT NULL |

---

## notification_db (Notification Service)

### notifications
| Column | Type | Constraints |
|---|---|---|
| notification_id | UUID (PK) | NOT NULL |
| recipient_id | UUID | NOT NULL |
| actor_id | UUID | NULLABLE |
| type | ENUM(NEW_COMMENT, COMMENT_REPLY, ADMIN_BROADCAST, ...) | NOT NULL |
| title | VARCHAR(255) | NOT NULL |
| message | VARCHAR(1000) | NOT NULL |
| related_id | VARCHAR(255) | NULLABLE |
| related_type | VARCHAR(255) | NULLABLE |
| is_read | BOOLEAN | NOT NULL, DEFAULT false |
| created_at | DATETIME | NOT NULL |

### audit_logs (notification)
| Column | Type | Constraints |
|---|---|---|
| audit_id | UUID (PK) | NOT NULL |
| actor_id | UUID | NULLABLE |
| action | VARCHAR(255) | NOT NULL |
| source | VARCHAR(255) | NOT NULL |
| details | VARCHAR(1500) | NOT NULL |
| created_at | DATETIME | NOT NULL |
