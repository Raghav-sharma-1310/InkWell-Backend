/*
 * This source file contains cross-service client communication for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.client;

import com.inkwell.comment.dto.ApiResponse;
import com.inkwell.comment.dto.response.PostMetaResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-service", path = "/api/posts/internal")
/* This interface groups post client behavior so the module keeps a clear responsibility. */
public interface PostClient {
    @GetMapping("/{postId}/meta")
    ApiResponse<PostMetaResponse> getMeta(@PathVariable("postId") UUID postId);
}
