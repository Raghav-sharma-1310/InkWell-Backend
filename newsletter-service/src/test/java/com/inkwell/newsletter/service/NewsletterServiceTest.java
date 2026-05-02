/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.service;

import com.inkwell.newsletter.dto.request.CampaignRequest;
import com.inkwell.newsletter.dto.request.SubscribeRequest;
import com.inkwell.newsletter.dto.response.SubscriberResponse;
import com.inkwell.newsletter.entity.Subscriber;
import com.inkwell.newsletter.enumtype.SubscriberStatus;
import com.inkwell.newsletter.repository.CampaignRepository;
import com.inkwell.newsletter.repository.SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups newsletter service test behavior so the module keeps a clear responsibility. */
class NewsletterServiceTest {

    @Mock private SubscriberRepository subscriberRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private MailService mailService;
    @Mock private TemplateService templateService;

    @InjectMocks private NewsletterService newsletterService;

    private SubscribeRequest subscribeRequest;

    @BeforeEach
    void setUp() {
        subscribeRequest = new SubscribeRequest("test@inkwell.com", "Test Subscriber", "technology,productivity");
    }

    @Test
    @DisplayName("Should subscribe new user successfully")
    void subscribeSuccess() {
        when(subscriberRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(invocation -> {
            Subscriber s = invocation.getArgument(0);
            s.setSubscriberId(UUID.randomUUID());
            return s;
        });

        SubscriberResponse response = newsletterService.subscribe(subscribeRequest, null);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@inkwell.com");
        assertThat(response.status()).isEqualTo(SubscriberStatus.PENDING);
        verify(mailService).send(eq("test@inkwell.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should subscribe with userId")
    void subscribeWithUserId() {
        UUID userId = UUID.randomUUID();
        when(subscriberRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(inv -> {
            Subscriber s = inv.getArgument(0);
            s.setSubscriberId(UUID.randomUUID());
            return s;
        });

        SubscriberResponse response = newsletterService.subscribe(subscribeRequest, userId.toString());
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should reject already active subscriber")
    void subscribeAlreadyActive() {
        Subscriber existing = Subscriber.builder()
                .email("test@inkwell.com")
                .status(SubscriberStatus.ACTIVE)
                .build();
        when(subscriberRepository.findByEmailIgnoreCase("test@inkwell.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> newsletterService.subscribe(subscribeRequest, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already subscribed");
    }

    @Test
    @DisplayName("Should re-subscribe previously unsubscribed user")
    void reSubscribe() {
        Subscriber existing = Subscriber.builder()
                .email("test@inkwell.com")
                .status(SubscriberStatus.UNSUBSCRIBED)
                .token("old-token")
                .build();
        when(subscriberRepository.findByEmailIgnoreCase("test@inkwell.com")).thenReturn(Optional.of(existing));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(existing);

        SubscriberResponse response = newsletterService.subscribe(subscribeRequest, null);
        assertThat(response.status()).isEqualTo(SubscriberStatus.PENDING);
    }

    @Test
    @DisplayName("Should confirm subscription")
    void confirmSuccess() {
        String token = "valid-token";
        Subscriber subscriber = Subscriber.builder()
                .token(token)
                .email("test@inkwell.com")
                .status(SubscriberStatus.PENDING)
                .build();

        when(subscriberRepository.findByToken(token)).thenReturn(Optional.of(subscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(subscriber);

        SubscriberResponse response = newsletterService.confirm(token);

        assertThat(response.status()).isEqualTo(SubscriberStatus.ACTIVE);
        verify(mailService).send(eq("test@inkwell.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject confirming already active subscription")
    void confirmAlreadyActive() {
        Subscriber subscriber = Subscriber.builder().token("token").email("t@t.com").status(SubscriberStatus.ACTIVE).build();
        when(subscriberRepository.findByToken("token")).thenReturn(Optional.of(subscriber));

        assertThatThrownBy(() -> newsletterService.confirm("token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already confirmed");
    }

    @Test
    @DisplayName("Should reject invalid confirmation token")
    void confirmInvalidToken() {
        when(subscriberRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsletterService.confirm("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should unsubscribe using adminUnsubscribe")
    void adminUnsubscribe() {
        UUID id = UUID.randomUUID();
        Subscriber subscriber = Subscriber.builder().subscriberId(id).email("t@t.com").status(SubscriberStatus.ACTIVE).build();
        when(subscriberRepository.findById(id)).thenReturn(Optional.of(subscriber));

        newsletterService.adminUnsubscribe(id);

        verify(subscriberRepository).save(argThat(s -> s.getStatus() == SubscriberStatus.UNSUBSCRIBED));
    }

    @Test
    @DisplayName("Should unsubscribe")
    void unsubscribe() {
        Subscriber subscriber = Subscriber.builder().token("token").email("t@t.com").status(SubscriberStatus.ACTIVE).build();
        when(subscriberRepository.findByToken("token")).thenReturn(Optional.of(subscriber));

        newsletterService.unsubscribe("token");

        verify(subscriberRepository).save(argThat(s -> s.getStatus() == SubscriberStatus.UNSUBSCRIBED));
    }

    @Test
    @DisplayName("Should list all subscribers")
    void all() {
        Subscriber s = Subscriber.builder().subscriberId(UUID.randomUUID()).email("t@t.com").status(SubscriberStatus.ACTIVE).build();
        when(subscriberRepository.findAll()).thenReturn(List.of(s));

        assertThat(newsletterService.all()).hasSize(1);
    }

    @Test
    @DisplayName("Should get user subscription status")
    void getMyStatus() {
        UUID userId = UUID.randomUUID();
        Subscriber s = Subscriber.builder().subscriberId(UUID.randomUUID()).email("t@t.com").userId(userId).status(SubscriberStatus.ACTIVE).build();
        when(subscriberRepository.findByUserId(userId)).thenReturn(Optional.of(s));

        SubscriberResponse result = newsletterService.getMyStatus(userId);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should return null when no subscription status")
    void getMyStatusNull() {
        UUID userId = UUID.randomUUID();
        when(subscriberRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThat(newsletterService.getMyStatus(userId)).isNull();
    }

    @Test
    @DisplayName("Should send campaign to active subscribers")
    void sendCampaign() {
        CampaignRequest request = new CampaignRequest("Test Subject", "Test content");
        Subscriber s = Subscriber.builder().email("reader@inkwell.com").token("tok").status(SubscriberStatus.ACTIVE).build();
        when(subscriberRepository.findByStatus(SubscriberStatus.ACTIVE)).thenReturn(List.of(s));

        newsletterService.sendCampaign(request);

        verify(campaignRepository).save(any());
        verify(mailService).send(eq("reader@inkwell.com"), eq("Test Subject"), anyString());
    }

    @Test
    @DisplayName("Should handle post published event")
    void onPostPublished() {
        Subscriber s = Subscriber.builder().email("reader@inkwell.com").token("tok").status(SubscriberStatus.ACTIVE).build();
        when(subscriberRepository.findByStatus(SubscriberStatus.ACTIVE)).thenReturn(List.of(s));

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "New Post");
        payload.put("slug", "new-post");
        payload.put("excerpt", "Excerpt text");
        payload.put("authorName", "Author");

        newsletterService.onPostPublished(payload);

        verify(mailService).send(eq("reader@inkwell.com"), contains("New Post"), anyString());
    }
}
