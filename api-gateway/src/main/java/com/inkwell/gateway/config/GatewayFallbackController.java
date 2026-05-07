/*
 * This source file contains gateway fallback behavior for the Inkwell platform.
 * The comments explain how circuit breaker failures are converted into a stable client response.
 */
package com.inkwell.gateway.config;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
/* This class groups gateway fallback behavior so downstream outages do not break the gateway contract. */
public class GatewayFallbackController {

    @SuppressWarnings("java:S3752")
    @RequestMapping("/fallback")
    // Performs the fallback workflow so circuit breaker failures return a predictable response.
    public Mono<ResponseEntity<Map<String, Object>>> fallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
            "error", "Service temporarily unavailable",
            "message", "The requested service is unavailable or responding slowly. Please try again shortly.",
            "path", path
        )));
    }
}
