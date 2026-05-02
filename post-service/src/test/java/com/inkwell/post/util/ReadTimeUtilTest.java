/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups read time util test behavior so the module keeps a clear responsibility. */
class ReadTimeUtilTest {

    @Test
    @DisplayName("Should estimate read time for short content")
    void shortContent() {
        int result = ReadTimeUtil.estimate("Hello world");
        assertThat(result).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should estimate read time for longer content")
    void longerContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("word ");
        }
        int result = ReadTimeUtil.estimate(sb.toString());
        assertThat(result).isGreaterThan(1);
    }

    @Test
    @DisplayName("Should handle HTML tags in content")
    void htmlContent() {
        int result = ReadTimeUtil.estimate("<p>Hello <strong>world</strong></p>");
        assertThat(result).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should handle empty content")
    void emptyContent() {
        int result = ReadTimeUtil.estimate("");
        assertThat(result).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should handle null content")
    void nullContent() {
        int result = ReadTimeUtil.estimate(null);
        assertThat(result).isGreaterThanOrEqualTo(0);
    }
}
