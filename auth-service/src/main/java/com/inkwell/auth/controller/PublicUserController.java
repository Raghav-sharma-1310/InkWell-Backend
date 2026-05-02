/*
 * Codex documentation pass: this source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.service.AuthService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/public")
@RequiredArgsConstructor
/* This class groups public user controller behavior so the module keeps a clear responsibility. */
public class PublicUserController {

    private final AuthService authService;

    @GetMapping("/authors")
    // Provides authors wiring so the framework can apply the expected runtime behavior.
    public ApiResponse<List<UserResponse>> authors() {
        return ApiResponse.of("Authors fetched", authService.getAuthors());
    }

    @GetMapping("/search")
    // Defines search so related behavior stays grouped in one place.
    public ApiResponse<List<UserResponse>> search(@RequestParam(name = "query", defaultValue = "") String query) {
        return ApiResponse.of("Users fetched", authService.searchUsers(query));
    }

    @GetMapping("/users/{userId}")
    // Defines user by id so related behavior stays grouped in one place.
    public ApiResponse<UserResponse> userById(@PathVariable UUID userId) {
        return ApiResponse.of("User fetched", authService.getUserResponse(userId));
    }
}
