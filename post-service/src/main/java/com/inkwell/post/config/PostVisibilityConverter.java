/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.config;

import com.inkwell.post.enumtype.PostVisibility;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Safe JPA converter for PostVisibility that gracefully handles unknown/removed
 * enum values stored in the database (e.g., EARLY_ACCESS).
 *
 * Instead of throwing "No enum constant" and crashing, this converter maps any
 * unrecognized value to PUBLIC and logs a warning.
 *
 * This is the migration-safe approach: even if the SQL migration hasn't been run
 * yet, the application will still start and serve requests.
 */
@Slf4j
@Converter(autoApply = true)
public class PostVisibilityConverter implements AttributeConverter<PostVisibility, String> {

    @Override
    // Defines convert to database column so related behavior stays grouped in one place.
    public String convertToDatabaseColumn(PostVisibility attribute) {
        if (attribute == null) {
            return PostVisibility.PUBLIC.name();
        }
        return attribute.name();
    }

    @Override
    // Defines convert to entity attribute so related behavior stays grouped in one place.
    public PostVisibility convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return PostVisibility.PUBLIC;
        }
        try {
            return PostVisibility.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown PostVisibility value '{}' found in database — defaulting to PUBLIC. "
                + "Run the migration script to fix: docs/V1__migrate_early_access_visibility.sql", dbData);
            return PostVisibility.PUBLIC;
        }
    }
}
