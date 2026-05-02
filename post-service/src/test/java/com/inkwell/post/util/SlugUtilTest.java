/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups slug util test behavior so the module keeps a clear responsibility. */
class SlugUtilTest {

    @Test
    @DisplayName("Should convert title to slug")
    void basicSlug() {
        assertThat(SlugUtil.toSlug("Hello World")).isEqualTo("hello-world");
    }

    @Test
    @DisplayName("Should handle special characters")
    void specialCharacters() {
        String slug = SlugUtil.toSlug("Spring Boot & JPA: A Guide!");
        assertThat(slug).doesNotContain("&", ":", "!")
                        .matches("[a-z0-9-]+");
    }

    @Test
    @DisplayName("Should handle multiple spaces")
    void multipleSpaces() {
        String slug = SlugUtil.toSlug("Hello   World");
        assertThat(slug).doesNotContain("--");
    }

    @Test
    @DisplayName("Should handle leading/trailing spaces")
    void trimmedSlug() {
        String slug = SlugUtil.toSlug("  Hello World  ");
        assertThat(slug).doesNotStartWith("-")
                        .doesNotEndWith("-");
    }
}
