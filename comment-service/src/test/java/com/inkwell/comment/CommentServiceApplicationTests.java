/*
 * This source file contains automated verification for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups comment service application tests behavior so the module keeps a clear responsibility. */
class CommentServiceApplicationTests {

    @Test
    void applicationClassExists() {
        assertThat(CommentServiceApplication.class).isNotNull();
    }
}
