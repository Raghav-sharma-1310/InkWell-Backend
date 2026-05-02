/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups html sanitizer test behavior so the module keeps a clear responsibility. */
class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    @DisplayName("Should keep basic formatting tags")
    void keepBasicTags() {
        String input = "<p>Hello <b>world</b></p>";
        String result = sanitizer.sanitize(input);
        assertThat(result).contains("<b>world</b>");
    }

    @Test
    @DisplayName("Should strip script tags")
    void stripScripts() {
        String result = sanitizer.sanitize("<script>alert('xss')</script>Hello");
        assertThat(result).doesNotContain("<script>")
                          .contains("Hello");
    }

    @Test
    @DisplayName("Should strip onclick handlers")
    void stripOnclick() {
        String result = sanitizer.sanitize("<a onclick=\"alert('xss')\" href=\"#\">Click</a>");
        assertThat(result).doesNotContain("onclick");
    }

    @Test
    @DisplayName("Should handle empty input")
    void emptyInput() {
        assertThat(sanitizer.sanitize("")).isEmpty();
    }

    @Test
    @DisplayName("Should handle plain text")
    void plainText() {
        assertThat(sanitizer.sanitize("Hello World")).isEqualTo("Hello World");
    }
}
