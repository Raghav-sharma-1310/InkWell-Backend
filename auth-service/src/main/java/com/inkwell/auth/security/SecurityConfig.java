/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.security;

import com.inkwell.auth.service.OAuth2AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
/* This class groups security config behavior so the module keeps a clear responsibility. */
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2AccountService oAuth2AccountService;

    @Bean
    // Provides security filter chain wiring so the framework can apply the expected runtime behavior.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh",
                    "/api/auth/forgot-password",
                    "/api/auth/verify-otp",
                    "/api/auth/reset-password",
                    "/api/auth/public/**",
                    "/api/auth/internal/**",
                    "/oauth2/**",
                    "/login/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"message\":\"Unauthorized\"}");
                })
            )
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2AccountService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler((request, response, exception) -> {
                    String frontendUrl = request.getScheme() + "://" + request.getServerName() + ":5173";
                    response.sendRedirect(frontendUrl + "/login?error=oauth");
                })
            )
            .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    // Defines password encoder so related behavior stays grouped in one place.
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
