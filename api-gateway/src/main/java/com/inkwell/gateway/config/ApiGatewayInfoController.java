/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.gateway.config;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
/* This class groups api gateway info controller behavior so the module keeps a clear responsibility. */
public class ApiGatewayInfoController {

    @GetMapping("/")
    // Defines root so related behavior stays grouped in one place.
    public Map<String, Object> root() {
        return Map.of(
            "service", "api-gateway",
            "status", "UP",
            "frontend", "http://localhost:5173",
            "actuator", "/actuator/health",
            "routes", "/actuator/gateway/routes",
            "swagger-ui", "/swagger-ui.html"
        );
    }
}
