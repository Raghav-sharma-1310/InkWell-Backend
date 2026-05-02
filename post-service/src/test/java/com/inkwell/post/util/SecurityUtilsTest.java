/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.util;

import com.inkwell.post.security.GatewayUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/* This class groups security utils test behavior so the module keeps a clear responsibility. */
class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsGatewayPrincipal() {
        GatewayUserPrincipal principal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "user", "user@test.com", "READER", "FREE", null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        assertThat(SecurityUtils.currentPrincipal()).isEqualTo(principal);
    }

    @Test
    void rejectsMissingGatewayPrincipal() {
        assertThatThrownBy(SecurityUtils::currentPrincipal)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated principal available");
    }
}
