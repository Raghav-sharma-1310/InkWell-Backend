/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
/* This class groups security config behavior so the module keeps a clear responsibility. */
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @Bean
    // Provides security filter chain wiring so the framework can apply the expected runtime behavior.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/api/posts/public/**", "/api/posts/explore/**", "/api/posts/internal/**", "/api/posts/authors/*/followers/count", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/**")
                .permitAll()
                .requestMatchers("/api/posts/reader/**").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
