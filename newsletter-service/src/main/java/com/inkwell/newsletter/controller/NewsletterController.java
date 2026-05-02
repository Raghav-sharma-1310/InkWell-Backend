/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.controller;

import com.inkwell.newsletter.dto.ApiResponse;
import com.inkwell.newsletter.dto.request.CampaignRequest;
import com.inkwell.newsletter.dto.request.SubscribeRequest;
import com.inkwell.newsletter.dto.response.SubscriberResponse;
import com.inkwell.newsletter.service.NewsletterService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
/* This class groups newsletter controller behavior so the module keeps a clear responsibility. */
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/public/subscribe")
    // Defines subscribe so related behavior stays grouped in one place.
    public ApiResponse<SubscriberResponse> subscribe(@Valid @RequestBody SubscribeRequest request, @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String currentUserId) { return ApiResponse.of("Subscription requested", newsletterService.subscribe(request, currentUserId)); }

    @GetMapping("/me")
    // Performs the get my status workflow so callers do not duplicate this logic.
    public ApiResponse<SubscriberResponse> getMyStatus(@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id") String currentUserId) { return ApiResponse.of("My newsletter status", newsletterService.getMyStatus(java.util.UUID.fromString(currentUserId))); }

    @GetMapping("/public/confirm")
    // Performs the verify workflow so callers do not duplicate this logic.
    public ApiResponse<SubscriberResponse> verify(@org.springframework.web.bind.annotation.RequestParam String token) { return ApiResponse.of("Subscription verified", newsletterService.confirm(token)); }

    @GetMapping("/public/unsubscribe/{token}")
    // Defines unsubscribe so related behavior stays grouped in one place.
    public ApiResponse<Void> unsubscribe(@PathVariable String token) { newsletterService.unsubscribe(token); return ApiResponse.of("Unsubscribed successfully", null); }

    @GetMapping("/admin/subscribers")
    @PreAuthorize("hasRole('ADMIN')")
    // Defines subscribers so related behavior stays grouped in one place.
    public ApiResponse<List<SubscriberResponse>> subscribers() { return ApiResponse.of("Subscribers fetched", newsletterService.all()); }

    @org.springframework.web.bind.annotation.PatchMapping("/admin/subscribers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    // Defines unsubscribe user so related behavior stays grouped in one place.
    public ApiResponse<Void> unsubscribeUser(@PathVariable java.util.UUID id) { newsletterService.adminUnsubscribe(id); return ApiResponse.of("User unsubscribed", null); }


    @PostMapping("/admin/campaigns")
    @PreAuthorize("hasRole('ADMIN')")
    // Defines campaign so related behavior stays grouped in one place.
    public ApiResponse<Void> campaign(@Valid @RequestBody CampaignRequest request) { 
        String message = newsletterService.sendCampaign(request); 
        return ApiResponse.of(message, null); 
    }
}
