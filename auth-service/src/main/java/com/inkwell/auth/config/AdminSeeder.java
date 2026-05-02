/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.config;

import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.AuthProvider;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.enumtype.SubscriptionTier;
import com.inkwell.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default ADMIN user on application startup if none exists.
 * The admin password is read from the configuration property {@code app.admin.default-password}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
/* This class groups admin seeder behavior so the module keeps a clear responsibility. */
public class AdminSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@inkwell.dev";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-password:Ch@ngeMe123!}")
    private String defaultAdminPassword;

    @Override
    // Defines run so related behavior stays grouped in one place.
    public void run(String... args) {
        // Check if ANY admin already exists (by email)
        if (userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).isPresent()) {
            log.info("Admin user ({}) already exists — skipping seed.", ADMIN_EMAIL);
            return;
        }

        User admin = User.builder()
            .username("admin")
            .email(ADMIN_EMAIL)
            .passwordHash(passwordEncoder.encode(defaultAdminPassword))
            .fullName("InkWell Admin")
            .role(Role.ADMIN)
            .provider(AuthProvider.LOCAL)
            .active(true)
            .subscriptionTier(SubscriptionTier.PRO)
            .build();

        userRepository.save(admin);
        log.info("══════════════════════════════════════════════");
        log.info("  DEFAULT ADMIN ACCOUNT CREATED");
        log.info("  Email:    {}", ADMIN_EMAIL);
        log.info("  Password: (set via app.admin.default-password)");
        log.info("  Role:     ADMIN");
        log.info("══════════════════════════════════════════════");
    }
}
