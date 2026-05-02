/*
 * Codex documentation pass: this source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.config;

import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.AuthProvider;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds demo users ONLY if they do not already exist.
 * Never truncates or deletes any existing data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
/* This class groups data initializer behavior so the module keeps a clear responsibility. */
public class DataInitializer implements CommandLineRunner {

    public static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID AUTHOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID READER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    // Defines run so related behavior stays grouped in one place.
    public void run(String... args) {
        seedIfMissing(ADMIN_ID, "admin", "admin@inkwell.dev", "Admin@123", "Platform Admin", Role.ADMIN);
        seedIfMissing(AUTHOR_ID, "author", "author@inkwell.dev", "Author@123", "Ava Author", Role.AUTHOR);
        seedIfMissing(READER_ID, "reader", "reader@inkwell.dev", "Reader@123", "Ryan Reader", Role.READER);
    }

    // Defines seed if missing so related behavior stays grouped in one place.
    private void seedIfMissing(UUID id, String username, String email, String password, String fullName, Role role) {
        // Skip if user already exists by email OR by ID
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.debug("Demo user '{}' already exists — skipping.", email);
            return;
        }
        if (userRepository.findById(id).isPresent()) {
            log.debug("Demo user ID {} already exists — skipping.", id);
            return;
        }

        User user = User.builder()
            .userId(id)
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .fullName(fullName)
            .role(role)
            .provider(AuthProvider.LOCAL)
            .active(true)
            .bio("Seeded demo account for InkWell showcase")
            .build();

        userRepository.save(user);
        log.info("Seeded demo {} user: {} ({})", role, fullName, email);
    }
}
