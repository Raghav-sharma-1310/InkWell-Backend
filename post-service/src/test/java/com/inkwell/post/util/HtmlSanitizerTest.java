/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups html sanitizer test behavior so the module keeps a clear responsibility. */
class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    @DisplayName("Should preserve safe HTML tags")
    void preserveSafeTags() {
        String input = "<p>Hello <strong>world</strong></p>";
        String result = sanitizer.sanitize(input);
        assertThat(result).contains("<p>").contains("<strong>");
    }

    @Test
    @DisplayName("Should strip script tags")
    void stripScriptTags() {
        String input = "<p>Hello</p><script>alert('xss')</script>";
        String result = sanitizer.sanitize(input);
        assertThat(result).doesNotContain("<script>")
                          .contains("<p>Hello</p>");
    }

    @Test
    @DisplayName("Should preserve code blocks")
    void preserveCodeBlocks() {
        String input = "<pre><code>System.out.println();</code></pre>";
        String result = sanitizer.sanitize(input);
        assertThat(result).contains("<code>");
    }

    @Test
    @DisplayName("Should preserve image tags with src and alt")
    void preserveImages() {
        String input = "<img src=\"https://cdn.inkwell.dev/test.jpg\" alt=\"test image\">";
        String result = sanitizer.sanitize(input);
        assertThat(result).contains("src=")
                          .contains("alt=");
    }

    @Test
    @DisplayName("Should strip event handlers")
    void stripEventHandlers() {
        String input = "<a href=\"#\" onclick=\"alert('xss')\">Click</a>";
        String result = sanitizer.sanitize(input);
        assertThat(result).doesNotContain("onclick");
    }

    @Test
    @DisplayName("Should handle empty string")
    void emptyString() {
        assertThat(sanitizer.sanitize("")).isEmpty();
    }
}
