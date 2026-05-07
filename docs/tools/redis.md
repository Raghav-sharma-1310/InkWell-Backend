# Redis Usage in InkWell

## Overview
Redis 7.4 (Alpine) is used for two purposes in InkWell: **API Gateway rate limiting** and **Post Service caching**.

## Configuration
- **Image**: `redis:7.4-alpine`
- **Port**: 6379
- **No authentication** configured by default (development mode)

## Usage by Service

### 1. API Gateway — Rate Limiting
- **Purpose**: Throttle requests to the newsletter endpoints
- **Mechanism**: Spring Cloud Gateway's built-in `RequestRateLimiter` filter
- **Config**: `redis-rate-limiter.replenishRate: 10`, `burstCapacity: 20`
- **Key Resolver**: `userOrIpKeyResolver` — rate limits by authenticated user ID or client IP
- **Applied to**: `/api/newsletter/**` routes only

### 2. Post Service — Caching
- **Purpose**: Cache frequently-accessed post data
- **Mechanism**: Spring Data Redis with `CacheConfig` class
- **Config**: `spring.data.redis.host` and `spring.data.redis.port` in `application.yml`
- **Implementation**: `com.inkwell.post.config.CacheConfig`

## Services NOT Using Redis
- auth-service: No direct interaction (has redis config in YAML but no active cache usage found)
- comment-service: No direct interaction
- category-service: No direct interaction
- media-service: No direct interaction
- newsletter-service: No direct interaction
- notification-service: No direct interaction
- payment-service: No direct interaction
