/*
 * This source file contains shared helper behavior for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
/* This class groups html sanitizer behavior so the module keeps a clear responsibility. */
public class HtmlSanitizer {

    // Defines sanitize so related behavior stays grouped in one place.
    public String sanitize(String html) {
        Safelist safelist = Safelist.relaxed()
            .addTags("pre", "code")
            .addAttributes("img", "src", "alt")
            .addAttributes("a", "href", "target", "rel");
        return Jsoup.clean(html, safelist);
    }
}
