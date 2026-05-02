/*
 * Codex documentation pass: this source file contains automated verification for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.mapper;

import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.AuthProvider;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.enumtype.SubscriptionTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups user mapper test behavior so the module keeps a clear responsibility. */
class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    @DisplayName("Should map User entity to UserResponse DTO")
    void toResponse() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .userId(id)
                .username("mapperuser")
                .email("mapper@inkwell.com")
                .fullName("Mapper User")
                .role(Role.AUTHOR)
                .bio("I map things")
                .avatarUrl("http://avatar.com")
                .phoneNumber("+1234567890")
                .provider(AuthProvider.LOCAL)
                .active(true)
                .createdAt(now)
                .subscriptionTier(SubscriptionTier.PRO)
                .build();

        UserResponse response = userMapper.toResponse(user);

        assertThat(response.userId()).isEqualTo(id);
        assertThat(response.username()).isEqualTo("mapperuser");
        assertThat(response.email()).isEqualTo("mapper@inkwell.com");
        assertThat(response.fullName()).isEqualTo("Mapper User");
        assertThat(response.role()).isEqualTo(Role.AUTHOR);
        assertThat(response.bio()).isEqualTo("I map things");
        assertThat(response.avatarUrl()).isEqualTo("http://avatar.com");
        assertThat(response.phoneNumber()).isEqualTo("+1234567890");
        assertThat(response.provider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.subscriptionTier()).isEqualTo(SubscriptionTier.PRO);
    }
}
