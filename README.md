# InkWell Platform — Design Documentation

> **Production-level documentation** for the InkWell microservices-based blogging platform.
> Suitable for developer onboarding, sprint evaluation, architecture review, hackathon presentation, and long-term maintenance.

---

## Platform Overview

InkWell is a **full-stack blogging platform** built with a microservices architecture using **Spring Boot 3.3 / Spring Cloud 2023**, a **React (Vite)** frontend, and **Docker Compose** for orchestration. It supports multi-role user management (Reader, Author, Admin, Default Admin), rich content creation, subscription-based premium features, and event-driven notifications.

### Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3.2, Spring Cloud 2023.0.3 |
| Frontend | React 18, Vite, TailwindCSS |
| Database | MySQL 8.4 (per-service schemas) |
| Caching | Redis 7.4 |
| Messaging | RabbitMQ 3.13 |
| Email | Mailpit (dev) / SMTP (prod) |
| Service Discovery | Eureka (Netflix OSS) |
| API Gateway | Spring Cloud Gateway |
| Monitoring | Spring Boot Admin Server |
| Code Quality | SonarQube + JaCoCo |
| Containerization | Docker + Docker Compose |
| Auth | JWT + OAuth2 (Google, GitHub) |
| Payments | Razorpay |

### Microservices

| Service | Port | Database | Description |
|---|---|---|---|
| discovery-service | 8761 | — | Eureka service registry |
| admin-server | 9090 | — | Spring Boot Admin monitoring |
| api-gateway | 8080 | — | Edge routing, JWT validation, rate limiting |
| auth-service | 8081 | auth_db | Authentication, users, payments, feedback |
| post-service | 8082 | post_db | Posts, likes, bookmarks, follows, reading history |
| category-service | 8083 | category_db | Categories, tags, post-taxonomy mappings |
| comment-service | 8084 | comment_db | Comments, comment likes |
| media-service | 8085 | media_db | File uploads (local / S3) |
| newsletter-service | 8086 | newsletter_db | Newsletter subscriptions, campaigns, emails |
| notification-service | 8087 | notification_db | In-app notifications, audit logs, emails |
| payment-service | 8088 | — | Razorpay order creation |
| frontend-web | 5173 | — | React SPA |

---

## Documentation Index

### Architecture
- [Whole-Project Architecture](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/architecture/whole-project-architecture.md)
- [Service Interaction Diagram](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/architecture/service-interaction-diagram.md)
- [Deployment & Tools Diagram](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/architecture/deployment-tools-diagram.md)

### Database
- [Database Design](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/database/database-design.md)
- [Database Service Ownership](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/database/database-service-ownership.md)

### Services
- [API Gateway](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/api-gateway/overview.md)
- [Auth Service](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/auth-service/overview.md)
- [Post Service](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/post-service/overview.md)
- [Comment Service](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/comment-service/overview.md)
- [Category Service](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/category-service/overview.md)
- [Media Service](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/media-service/overview.md)
- [Newsletter Service](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/newsletter-service/overview.md)
- [Notification Service](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/notification-service/overview.md)
- [Payment Service](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/payment-service/overview.md)
- [Discovery Service](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/discovery-service/overview.md)
- [Admin Server](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/services/admin-server/overview.md)

### Tools
- [RabbitMQ](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/tools/rabbitmq.md)
- [Redis](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/tools/redis.md)
- [Mailpit](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/tools/mailhog.md)
- [SonarQube](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/tools/sonarqube.md)
- [Docker](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/tools/docker.md)
- [Monitoring](https://github.com/Raghav-sharma-1310/InkWell-Backend/blob/Microservices-core/docs/tools/monitoring.md)

---

## Quick Start

```bash
# Clone and start all services
docker-compose up --build -d

# Frontend
cd frontend-web && npm install && npm run dev
```

> Default Admin: `admin@inkwell.dev` / password via `app.admin.default-password` config.
