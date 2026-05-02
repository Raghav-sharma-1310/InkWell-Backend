/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.AuthProvider;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/* This class groups oauth2 account service behavior so the module keeps a clear responsibility. */
public class OAuth2AccountService extends DefaultOAuth2UserService {

    private static final String EMAIL_KEY = "email";

    private final UserRepository userRepository;

    @Override
    // Performs the load user workflow so callers do not duplicate this logic.
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        return processUser(userRequest, oauth2User);
    }

    // Performs the process user workflow so callers do not duplicate this logic.
    public OAuth2User processUser(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        String resolvedEmail = (String) attributes.get(EMAIL_KEY);
        if (resolvedEmail == null && attributes.get("login") != null) {
            resolvedEmail = attributes.get("login") + "@github.local";
        }
        if (resolvedEmail == null) {
            throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("email_not_found"), "Unable to resolve email from OAuth2 provider");
        }
        String fullName = (String) attributes.getOrDefault("name", resolvedEmail);
        String username = resolvedEmail.substring(0, resolvedEmail.indexOf("@")).replaceAll("\\W", "");
        AuthProvider provider = "github".equalsIgnoreCase(registrationId) ? AuthProvider.GITHUB : AuthProvider.GOOGLE;
        final String email = resolvedEmail;
        Map<String, Object> mapped = new HashMap<>(attributes);
        mapped.put(EMAIL_KEY, email);

        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseGet(() -> userRepository.save(User.builder()
                .username(username)
                .email(email)
                .fullName(fullName)
                .passwordHash("OAUTH2_ACCOUNT")
                .role(Role.READER)
                .provider(provider)
                .active(true)
                .avatarUrl((String) attributes.get("avatar_url"))
                .build()));

        mapped.put("appUser", user);
        String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        if (nameAttributeKey == null || !mapped.containsKey(nameAttributeKey)) {
            nameAttributeKey = EMAIL_KEY;
        }
        return new DefaultOAuth2User(oauth2User.getAuthorities(), mapped, nameAttributeKey);
    }
}
