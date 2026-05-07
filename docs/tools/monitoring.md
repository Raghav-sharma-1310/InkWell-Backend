# Monitoring in InkWell

## Overview
InkWell uses **Spring Boot Admin Server** as the centralized monitoring dashboard for all microservices. Each service registers with the Admin Server and exposes health, metrics, and info via Spring Boot Actuator.

## Spring Boot Admin Server
- **Port**: 9090
- **UI Title**: InkWell Admin Server
- **URL**: http://localhost:9090
- **Discovers services via**: Eureka registry

### Features
- Real-time health status of all services
- JVM metrics (heap, threads, GC)
- HTTP request metrics
- Environment properties
- Log level management
- Circuit breaker status (for services with Resilience4j)

## Actuator Endpoints

### Exposed by all services:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

### Additional endpoints by API Gateway:
```yaml
include: health,info,metrics,gateway,circuitbreakers,circuitbreakerevents
```

### Additional endpoints by services with Resilience4j:
```yaml
include: health,info,metrics,circuitbreakers,circuitbreakerevents
```

## Resilience4j Circuit Breakers

### API Gateway
- **Instance**: `gatewayCircuitBreaker`
- Sliding window: 20 calls (COUNT_BASED)
- Failure threshold: 50%
- Slow call threshold: 50% (>4s)
- Wait in open state: 30s
- Half-open permits: 3
- Time limiter: 5s timeout

### Post Service (for category-service calls)
- Sliding window: 10 calls
- Failure threshold: 50%
- Retry: 3 attempts, 500ms wait
- Time limiter: 3s

### Comment & Notification Services
- Default config: sliding window 10, failure 50%, wait 30s, time limit 3s

## Service Registration Flow
```
Service starts → Registers with Eureka (8761) → Reports to Admin Server (9090)
                                                        ↓
                                                  Health polling via actuator
```

## Health Indicators
Each service reports:
- **UP/DOWN** status
- Database connection (MySQL)
- Redis connection (where applicable)
- RabbitMQ connection (where applicable)
- Disk space
- Circuit breaker state
