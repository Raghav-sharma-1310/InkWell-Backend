/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.security;

import java.util.UUID;

/* This record groups gateway user principal behavior so the module keeps a clear responsibility. */
public record GatewayUserPrincipal(String userId, String username, String email, String role) {
    // Defines user uuid so related behavior stays grouped in one place.
    public UUID userUuid() { return UUID.fromString(userId); }
}
