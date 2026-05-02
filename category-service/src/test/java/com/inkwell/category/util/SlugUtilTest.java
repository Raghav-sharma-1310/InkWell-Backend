/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups slug util test behavior so the module keeps a clear responsibility. */
class SlugUtilTest {

    @Test
    @DisplayName("Should convert basic text to slug")
    void basicSlug() {
        assertThat(SlugUtil.toSlug("Hello World")).isEqualTo("hello-world");
    }

    @Test
    @DisplayName("Should handle special characters")
    void specialChars() {
        assertThat(SlugUtil.toSlug("Hello! @World#")).isEqualTo("hello-world");
    }

    @Test
    @DisplayName("Should handle multiple spaces")
    void multipleSpaces() {
        assertThat(SlugUtil.toSlug("Hello   World")).isEqualTo("hello-world");
    }

    @Test
    @DisplayName("Should handle already slugified text")
    void alreadySlug() {
        assertThat(SlugUtil.toSlug("hello-world")).isEqualTo("hello-world");
    }

    @Test
    @DisplayName("Should handle uppercase")
    void uppercase() {
        assertThat(SlugUtil.toSlug("JAVA SPRING BOOT")).isEqualTo("java-spring-boot");
    }

    @Test
    @DisplayName("Should handle numbers")
    void numbers() {
        assertThat(SlugUtil.toSlug("Top 10 Tips")).isEqualTo("top-10-tips");
    }

    @Test
    @DisplayName("Should handle multiple dashes")
    void multipleDashes() {
        assertThat(SlugUtil.toSlug("hello---world")).isEqualTo("hello-world");
    }
}
