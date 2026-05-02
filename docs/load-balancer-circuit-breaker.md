# Load Balancer and Circuit Breaker

## Disclaimer

This change is additive infrastructure behavior only. It must not replace existing service business logic, endpoint contracts, persistence behavior, authentication rules, or frontend flows.

## What Was Added

- API Gateway routes continue to use Eureka service names through `lb://...`, which lets Spring Cloud LoadBalancer distribute traffic across healthy service instances.
- API Gateway now has a Resilience4j circuit breaker filter with a `/fallback` response for downstream failures or slow services.
- Feign-based service-to-service calls now use Spring Cloud OpenFeign circuit breaker support.
- LoadBalancer cache and retry settings were added for gateway and Feign client modules.
- Actuator circuit breaker endpoints are exposed for services that use circuit breakers.

## Why It Was Added

- Load balancing keeps traffic from depending on one fixed service instance.
- Circuit breakers stop repeated calls to failing or slow downstream services, giving them time to recover.
- Fallback responses keep clients from receiving raw connection errors when a downstream service is temporarily unavailable.
