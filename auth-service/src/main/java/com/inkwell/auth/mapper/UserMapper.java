/*
 * Codex documentation pass: this source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.mapper;

import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
/* This class groups user mapper behavior so the module keeps a clear responsibility. */
public class UserMapper {

    // Defines to response so related behavior stays grouped in one place.
    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.getBio(),
            user.getAvatarUrl(),
            user.getPhoneNumber(),
            user.getProvider(),
            user.isActive(),
            user.getCreatedAt(),
            user.getSubscriptionTier(),
            user.getSubscriptionStatus(),
            user.getSubscriptionEndDate()
        );
    }
}
