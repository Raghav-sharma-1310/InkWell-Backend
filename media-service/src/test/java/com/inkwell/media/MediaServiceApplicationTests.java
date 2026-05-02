/*
 * This source file contains automated verification for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups media service application tests behavior so the module keeps a clear responsibility. */
class MediaServiceApplicationTests {

    @Test
    void applicationClassExists() {
        assertThat(MediaServiceApplication.class).isNotNull();
    }
}
