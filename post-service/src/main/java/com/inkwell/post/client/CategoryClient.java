/*
 * This source file contains cross-service client communication for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "category-service", path = "/api/categories/internal")
/* This interface groups category client behavior so the module keeps a clear responsibility. */
public interface CategoryClient {

    @PostMapping("/posts/{postId}/taxonomy")
    void syncTaxonomy(@PathVariable("postId") String postId, @RequestBody TaxonomySyncRequest request);
}
