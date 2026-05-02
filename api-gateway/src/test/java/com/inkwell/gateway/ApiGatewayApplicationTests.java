/*
 * Codex documentation pass: this source file contains automated verification for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups api gateway application tests behavior so the module keeps a clear responsibility. */
class ApiGatewayApplicationTests {

    @Test
    void applicationClassExists() {
        assertThat(ApiGatewayApplication.class).isNotNull();
    }
}
