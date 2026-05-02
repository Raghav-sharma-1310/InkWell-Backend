/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.util;

import org.jsoup.Jsoup;

/* This class groups read time util behavior so the module keeps a clear responsibility. */
public final class ReadTimeUtil {

    // Defines read time util so related behavior stays grouped in one place.
    private ReadTimeUtil() {
    }

    // Defines estimate so related behavior stays grouped in one place.
    public static int estimate(String htmlContent) {
        if (htmlContent == null) return 0;
        String plain = Jsoup.parse(htmlContent).text();
        int words = plain.isBlank() ? 0 : plain.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }
}
