# Admin Server — Service Interactions

## Inbound
| Source | Protocol | Purpose |
|---|---|---|
| All microservices | HTTP (Actuator) | Services report health/metrics to Admin Server |
| Developer browser | HTTP | Access monitoring dashboard |

## Outbound
| Target | Protocol | Purpose |
|---|---|---|
| Eureka | HTTP | Discover registered service instances |
| All microservices | HTTP | Poll `/actuator/health`, `/actuator/metrics`, `/actuator/info` |

## Tool Involvement
| Tool | Usage |
|---|---|
| Eureka | Service discovery |
| MySQL | No direct interaction |
| Redis | No direct interaction |
| RabbitMQ | No direct interaction |
| Mailpit | No direct interaction |
