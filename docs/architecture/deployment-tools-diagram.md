# Deployment & Tools Diagram

## Overview

InkWell uses **Docker Compose** to orchestrate 14 containers (12 services + MySQL + Redis + RabbitMQ + Mailpit). All infrastructure is defined in a single `docker-compose.yml` at the project root.

## Container Inventory

| Container | Image | Ports | Depends On | Purpose |
|---|---|---|---|---|
| mysql | mysql:8.4 | 3306 | — | Primary database |
| redis | redis:7.4-alpine | 6379 | — | Caching & rate limiting |
| rabbitmq | rabbitmq:3.13-management | 5672, 15672 | — | Message broker |
| mailpit | axllent/mailpit:latest | 1025, 8025 | — | Email capture (dev) |
| discovery-service | Custom (Dockerfile) | 8761 | — | Eureka registry |
| admin-server | Custom (Dockerfile) | 9090 | discovery-service | Spring Boot Admin |
| api-gateway | Custom (Dockerfile) | 8080 | discovery-service, admin-server, redis | Edge router |
| auth-service | Custom (Dockerfile) | — | mysql, discovery-service, admin-server | Authentication |
| post-service | Custom (Dockerfile) | — | mysql, redis, rabbitmq, discovery-service, admin-server | Posts |
| category-service | Custom (Dockerfile) | — | mysql, discovery-service, admin-server | Categories & Tags |
| comment-service | Custom (Dockerfile) | — | mysql, rabbitmq, discovery-service, admin-server | Comments |
| media-service | Custom (Dockerfile) | — | mysql, discovery-service, admin-server | File uploads |
| newsletter-service | Custom (Dockerfile) | — | mysql, rabbitmq, mailpit, discovery-service, admin-server | Newsletter |
| notification-service | Custom (Dockerfile) | — | mysql, rabbitmq, mailpit, discovery-service, admin-server | Notifications |
| frontend-web | Custom (Dockerfile) | 5173 | api-gateway | React SPA |

## External Services

| Service | Purpose | Configuration |
|---|---|---|
| Google OAuth2 | Social login | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` |
| GitHub OAuth2 | Social login | `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` |
| Razorpay | Payment processing | `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` |
| AWS S3 | Media storage (optional) | `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` |

## DevOps Tools

| Tool | Purpose | Integration |
|---|---|---|
| SonarQube | Static code analysis | Maven plugin `sonar-maven-plugin:4.0.0.4121` |
| JaCoCo | Code coverage | Maven plugin `jacoco-maven-plugin:0.8.12` |
| Docker Compose | Container orchestration | `docker-compose.yml` at project root |
| Maven | Build tool | Multi-module POM with Java 21 |

## Environment Variables

Key environment variables are externalized in `.env.example`:
- `JWT_SECRET` — shared between gateway and auth-service
- `MYSQL_ROOT_PASSWORD` — database root password
- `GOOGLE_CLIENT_ID/SECRET` — OAuth2
- `GITHUB_CLIENT_ID/SECRET` — OAuth2
- `RAZORPAY_KEY_ID/SECRET` — payment processing
- `STORAGE_MODE` — `local` or `s3` for media storage

## Diagram

See [deployment-tools-diagram.mmd](deployment-tools-diagram.mmd) for the Mermaid source.
