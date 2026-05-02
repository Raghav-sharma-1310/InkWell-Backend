# InkWell - Blogging Platform with Authentication and Admin Panel

InkWell is a production-style microservices blogging platform built with Spring Boot, Spring Cloud, MySQL, Redis, RabbitMQ, JWT security, OAuth2 login, React, Vite, and Tailwind CSS.

## Architecture Snapshot

- `discovery-service`: Eureka registry
- `admin-server`: Spring Boot Admin monitoring dashboard
- `api-gateway`: single entry point, CORS, JWT validation, routing, rate limiting
- `auth-service`: registration, login, refresh tokens, OAuth2 login, profile management, RBAC
- `post-service`: post lifecycle, feed, search, likes, analytics, Redis caching
- `comment-service`: threaded comments, moderation, likes, soft delete
- `category-service`: categories, tags, post-category mapping, trending tags
- `media-service`: upload library, local/S3 storage abstraction
- `newsletter-service`: double opt-in subscriptions, campaigns, post alerts
- `notification-service`: in-app notifications, email fan-out, audit log storage
- `frontend-web`: React + Tailwind responsive application

## Documentation

- [Architecture](./docs/architecture.md)
- [API Summary](./docs/api-summary.md)
- [Run Guide](./docs/run-guide.md)

## Demo Accounts

- Admin: `admin@inkwell.dev` / `Admin@123`
- Author: `author@inkwell.dev` / `Author@123`
- Reader: `reader@inkwell.dev` / `Reader@123`

## Start Fast

1. Copy `.env.example` to `.env`
2. Update secrets and OAuth values if needed
3. Run `docker compose up --build`
4. Run the frontend from `frontend-web` with `npm install` and `npm run dev`
5. Open Spring Boot Admin at `http://localhost:9090` and Eureka at `http://localhost:8761`

## What To Change Before Running

- Change database password if you do not want the default local demo password
- Add Google/GitHub OAuth credentials if social login is required
- Keep `STORAGE_MODE=local` for easiest setup, or switch to `s3` and add AWS values
- If ports are already in use on your machine, update them in `.env`

## Monitoring And Discovery

- Every backend service is connected to `discovery-service` using Eureka
- Every backend service is connected to `admin-server` using Spring Boot Admin client
- Each microservice has its own MySQL database/schema:
  - `auth_db`
  - `post_db`
  - `comment_db`
  - `category_db`
  - `media_db`
  - `newsletter_db`
  - `notification_db`

## Project Status

This repository is generated as a submission-ready end-to-end starter with seed data, Docker support, Swagger docs, service discovery, gateway routing, RabbitMQ events, Redis caching hooks, and responsive frontend flows for guest, reader, author, and admin usage.
