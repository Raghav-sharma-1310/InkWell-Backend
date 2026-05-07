# Docker Usage in InkWell

## Overview
Docker and Docker Compose are used to containerize and orchestrate all 14+ services and infrastructure components in the InkWell platform.

## Docker Compose Version
- **Version**: 3.9

## Containers

### Infrastructure
| Container | Image | Volumes | Ports |
|---|---|---|---|
| mysql | mysql:8.4 | `mysql-data:/var/lib/mysql`, `./docker/mysql-init:/docker-entrypoint-initdb.d` | 3306 |
| redis | redis:7.4-alpine | — | 6379 |
| rabbitmq | rabbitmq:3.13-management | — | 5672, 15672 |
| mailpit | axllent/mailpit:latest | — | 1025, 8025 |

### Platform Services
| Container | Build Context | Ports | Depends On |
|---|---|---|---|
| discovery-service | `./discovery-service` | 8761 | — |
| admin-server | `./admin-server` | 9090 | discovery-service |
| api-gateway | `./api-gateway` | 8080 | discovery-service, admin-server, redis |

### Business Services
| Container | Build Context | Depends On |
|---|---|---|
| auth-service | `./auth-service` | mysql, discovery-service, admin-server |
| post-service | `./post-service` | mysql, redis, rabbitmq, discovery-service, admin-server |
| category-service | `./category-service` | mysql, discovery-service, admin-server |
| comment-service | `./comment-service` | mysql, rabbitmq, discovery-service, admin-server |
| media-service | `./media-service` | mysql, discovery-service, admin-server |
| newsletter-service | `./newsletter-service` | mysql, rabbitmq, mailpit, discovery-service, admin-server |
| notification-service | `./notification-service` | mysql, rabbitmq, mailpit, discovery-service, admin-server |
| frontend-web | `./frontend-web` | api-gateway |

## Environment Variables
Key variables are set via `.env` file (see `.env.example`):
- `JWT_SECRET`, `MYSQL_ROOT_PASSWORD`
- OAuth2 client credentials
- Razorpay credentials
- Storage mode (local/s3)

## Common Commands

```bash
# Start all services
docker-compose up --build -d

# View logs
docker-compose logs -f <service-name>

# Stop all
docker-compose down

# Rebuild single service
docker-compose up --build -d auth-service

# Reset database
docker-compose down -v  # removes volumes
```

## Named Volumes
- `mysql-data` — persists MySQL data across container restarts

## Init Scripts
- `./docker/mysql-init/` — SQL scripts executed on first MySQL startup (database creation)
