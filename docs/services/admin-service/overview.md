# Admin Server — Service Overview

## Purpose
The Admin Server is a **Spring Boot Admin** monitoring dashboard. It provides a web UI for monitoring the health, metrics, and configuration of all registered microservices. It is NOT a business service — it has no database, no APIs for end users, and no business logic.

> **Note**: This is the `admin-server` module (Spring Boot Admin), NOT a business-level "admin service". All admin business logic (user management, console) resides in the `auth-service`.

## Port: 9090 | Database: None

---

## Classes

| Class | Purpose |
|---|---|
| `AdminServerApplication` | Main class, annotated with `@EnableAdminServer` |
| `SecurityConfig` | Basic security configuration for the Admin UI |

## Config (application.yml)
```yaml
spring:
  application:
    name: admin-server
  boot:
    admin:
      ui:
        title: InkWell Admin Server
```

## Features
- Auto-discovers all services via **Eureka**
- Displays real-time health status
- Shows JVM metrics (memory, threads, GC)
- Provides log level management
- Shows environment properties
- Monitors circuit breaker states

## APIs Exposed
No business APIs. Only the Admin Server web UI at http://localhost:9090.

## External Tools
- **Eureka**: Discovers registered service instances
- No MySQL, Redis, RabbitMQ, or Mailpit interaction

---

## Diagrams
The Admin Server is a monitoring tool and does not have the typical controller-service-repository architecture. The diagrams below document its role in the overall system.
