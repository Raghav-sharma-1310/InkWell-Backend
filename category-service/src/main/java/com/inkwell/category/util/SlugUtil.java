/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.util;

public final class SlugUtil {
    // Defines slug util so related behavior stays grouped in one place.
    private SlugUtil() {}
    // Defines to slug so related behavior stays grouped in one place.
    public static String toSlug(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-").replaceAll("-+", "-");
    }
}
