/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.entity.User;
import com.inkwell.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/* This class groups oauth2 account service test behavior so the module keeps a clear responsibility. */
class OAuth2AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OAuth2AccountService service;

    // Performs the create request workflow so callers do not duplicate this logic.
    private OAuth2UserRequest createRequest(String registrationId, String userNameAttributeName) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("clientId")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/oauth2/authorize")
                .tokenUri("https://example.com/oauth2/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName(userNameAttributeName)
                .build();
        return new OAuth2UserRequest(registration, new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", null, null));
    }

    @Test
    @DisplayName("Should process user successfully with existing email")
    void processUserSuccess() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "test@inkwell.com");
        attrs.put("name", "Test User");
        OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attrs, "email");

        User existingUser = User.builder().email("test@inkwell.com").build();
        when(userRepository.findByEmailIgnoreCase("test@inkwell.com")).thenReturn(Optional.of(existingUser));

        OAuth2UserRequest req = createRequest("google", "email");
        OAuth2User result = service.processUser(req, oauth2User);

        assertThat((String) result.getAttribute("email")).isEqualTo("test@inkwell.com");
        assertThat((Object) result.getAttribute("appUser")).isNotNull();
    }

    @Test
    @DisplayName("Should handle missing email but fallback to github login")
    void processUserFallbackGithub() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("login", "octocat");
        OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attrs, "login");

        when(userRepository.findByEmailIgnoreCase("octocat@github.local")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OAuth2UserRequest req = createRequest("github", "login");
        OAuth2User result = service.processUser(req, oauth2User);

        assertThat((String) result.getAttribute("email")).isEqualTo("octocat@github.local");
    }

    @Test
    @DisplayName("Should throw exception if email cannot be resolved")
    void processUserNoEmail() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", "123");
        OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attrs, "id");

        OAuth2UserRequest req = createRequest("google", "id");
        assertThatThrownBy(() -> service.processUser(req, oauth2User))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Unable to resolve email");
    }
}
