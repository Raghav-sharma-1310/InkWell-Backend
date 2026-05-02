/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.inkwell.notification.security.GatewayUserPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/* This class groups security utils test behavior so the module keeps a clear responsibility. */
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentPrincipalReturnsGatewayPrincipalFromSecurityContext() {
        GatewayUserPrincipal principal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "reader",
            "reader@inkwell.com", "READER");
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));

        assertThat(SecurityUtils.currentPrincipal()).isSameAs(principal);
    }
}
