/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.util;

import com.inkwell.notification.security.GatewayUserPrincipal; import org.springframework.security.core.context.SecurityContextHolder;

/* This class groups security utils behavior so the module keeps a clear responsibility. */
public final class SecurityUtils { private SecurityUtils() {} public static GatewayUserPrincipal currentPrincipal() { return (GatewayUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); } }
