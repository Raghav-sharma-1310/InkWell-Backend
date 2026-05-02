/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.util;

import com.inkwell.auth.security.GatewayUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/* This class groups security utils behavior so the module keeps a clear responsibility. */
public final class SecurityUtils {

    // Provides security utils wiring so the framework can apply the expected runtime behavior.
    private SecurityUtils() {
    }

    // Defines current principal so related behavior stays grouped in one place.
    public static GatewayUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof GatewayUserPrincipal principal)) {
            throw new IllegalStateException("No authenticated principal available");
        }
        return principal;
    }
}
