/*
 * This source file contains cross-service client communication for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.client;

import com.inkwell.notification.dto.ApiResponse;
import com.inkwell.notification.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", path = "/api/auth")
/* This interface groups auth client behavior so the module keeps a clear responsibility. */
public interface AuthClient {
    @GetMapping("/internal/users/{userId}") ApiResponse<UserResponse> getUser(@PathVariable("userId") UUID userId);
    @GetMapping("/public/search") ApiResponse<List<UserResponse>> searchUsers(@RequestParam("query") String query);
}
