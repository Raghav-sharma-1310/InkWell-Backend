/*
 * This source file contains automated verification for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.admin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups admin server application tests behavior so the module keeps a clear responsibility. */
class AdminServerApplicationTests {

    @Test
    void applicationClassExists() {
        assertThat(AdminServerApplication.class).isNotNull();
    }
}
