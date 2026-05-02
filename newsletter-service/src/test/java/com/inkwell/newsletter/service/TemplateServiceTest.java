/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/* This class groups template service test behavior so the module keeps a clear responsibility. */
class TemplateServiceTest {

    private final TemplateService templateService = new TemplateService();

    @Test
    void rendersClasspathTemplateWithValues() {
        String rendered = templateService.render("mail/confirm.txt", Map.of("name", "Reader", "link", "https://inkwell.test/confirm"));

        assertThat(rendered).contains("Reader").contains("InkWell newsletter subscription");
    }

    @Test
    void wrapsMissingTemplateAsIllegalStateException() {
        String missingTemplate = "mail/missing.txt";
        Map<String, String> values = Map.of();

        assertThatThrownBy(() -> templateService.render(missingTemplate, values))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("mail/missing.txt");
    }
}
