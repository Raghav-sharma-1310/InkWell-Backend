/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inkwell.newsletter.dto.request.CampaignRequest;
import com.inkwell.newsletter.dto.request.SubscribeRequest;
import com.inkwell.newsletter.dto.response.SubscriberResponse;
import com.inkwell.newsletter.enumtype.SubscriberStatus;
import com.inkwell.newsletter.service.NewsletterService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
/* This class groups newsletter controller direct test behavior so the module keeps a clear responsibility. */
class NewsletterControllerDirectTest {

    @Mock
    private NewsletterService newsletterService;

    private NewsletterController newsletterController;

    @BeforeEach
    void setUp() {
        newsletterController = new NewsletterController(newsletterService);
    }

    @Test
    void subscribeDelegatesOptionalUserHeader() {
        UUID userId = UUID.randomUUID();
        SubscribeRequest request = new SubscribeRequest("reader@inkwell.com", "Reader", "weekly");
        SubscriberResponse response = response(userId, SubscriberStatus.PENDING);
        when(newsletterService.subscribe(request, userId.toString())).thenReturn(response);

        var result = newsletterController.subscribe(request, userId.toString());

        assertThat(result.message()).isEqualTo("Subscription requested");
        assertThat(result.data()).isEqualTo(response);
    }

    @Test
    void getMyStatusParsesUserIdHeader() {
        UUID userId = UUID.randomUUID();
        SubscriberResponse response = response(userId, SubscriberStatus.ACTIVE);
        when(newsletterService.getMyStatus(userId)).thenReturn(response);

        var result = newsletterController.getMyStatus(userId.toString());

        assertThat(result.message()).isEqualTo("My newsletter status");
        assertThat(result.data()).isEqualTo(response);
    }

    @Test
    void publicEndpointsDelegateByToken() {
        SubscriberResponse response = response(null, SubscriberStatus.ACTIVE);
        when(newsletterService.confirm("token-1")).thenReturn(response);

        assertThat(newsletterController.verify("token-1").data()).isEqualTo(response);
        assertThat(newsletterController.unsubscribe("token-2").message()).isEqualTo("Unsubscribed successfully");

        verify(newsletterService).confirm("token-1");
        verify(newsletterService).unsubscribe("token-2");
    }

    @Test
    void adminEndpointsReturnExpectedMessages() {
        UUID subscriberId = UUID.randomUUID();
        SubscriberResponse response = response(subscriberId, SubscriberStatus.ACTIVE);
        when(newsletterService.all()).thenReturn(List.of(response));
        when(newsletterService.sendCampaign(new CampaignRequest("Subject", "Body"))).thenReturn("Campaign sent");

        assertThat(newsletterController.subscribers().data()).containsExactly(response);
        assertThat(newsletterController.unsubscribeUser(subscriberId).message()).isEqualTo("User unsubscribed");
        assertThat(newsletterController.campaign(new CampaignRequest("Subject", "Body")).message()).isEqualTo("Campaign sent");

        verify(newsletterService).adminUnsubscribe(subscriberId);
    }

    @Test
    void serviceInfoReturnsHealthMetadata() {
        var result = new ServiceInfoController().root();

        assertThat(result).containsEntry("service", "newsletter-service")
            .containsEntry("status", "UP")
            .containsEntry("health", "/actuator/health");
    }

    // Defines response so related behavior stays grouped in one place.
    private static SubscriberResponse response(UUID userId, SubscriberStatus status) {
        return new SubscriberResponse(UUID.randomUUID(), "reader@inkwell.com", userId, "Reader", status,
            LocalDateTime.now(), null, "weekly");
    }
}
