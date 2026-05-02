/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
/* This class groups service info controller behavior so the module keeps a clear responsibility. */
public class ServiceInfoController {

    @GetMapping("/")
    // Defines root so related behavior stays grouped in one place.
    public Map<String, Object> root() {
        return Map.of(
            "service", "notification-service",
            "status", "UP",
            "docs", "/swagger-ui.html",
            "health", "/actuator/health"
        );
    }
}
