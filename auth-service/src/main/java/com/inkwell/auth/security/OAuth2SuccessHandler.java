/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.security;

import com.inkwell.auth.entity.User;
import com.inkwell.auth.service.EmailService;
import com.inkwell.auth.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
/* This class groups oauth2 success handler behavior so the module keeps a clear responsibility. */
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException, ServletException {
        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        User user = (User) principal.getAttributes().get("appUser");
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createForUser(user).getToken();
        String redirect = redirectUri
            + "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
            + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        emailService.sendLoginNotificationEmail(user.getEmail(), user.getFullName(), user.getProvider().name());
        response.sendRedirect(redirect);
    }
}
