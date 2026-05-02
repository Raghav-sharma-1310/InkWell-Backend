/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
/* This class groups template service behavior so the module keeps a clear responsibility. */
public class TemplateService {
    // Defines render so related behavior stays grouped in one place.
    public String render(String path, java.util.Map<String, String> values) {
        try {
            String template = Files.readString(Path.of(new ClassPathResource(path).getURI()));
            for (var entry : values.entrySet()) { template = template.replace("{{" + entry.getKey() + "}}", entry.getValue()); }
            return template;
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }
}
