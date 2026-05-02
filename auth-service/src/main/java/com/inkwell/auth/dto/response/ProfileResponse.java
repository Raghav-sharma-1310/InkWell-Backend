/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
/* This class groups profile response behavior so the module keeps a clear responsibility. */
public class ProfileResponse {
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String bio;
    private String avatarUrl;
    private String phoneNumber;
    private String subscriptionTier;
    private String subscriptionStatus;
    private LocalDateTime subscriptionEndDate;
    private LocalDateTime createdAt;
}
