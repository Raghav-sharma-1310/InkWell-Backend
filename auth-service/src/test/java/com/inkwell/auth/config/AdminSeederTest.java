/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.config;

import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.enumtype.SubscriptionTier;
import com.inkwell.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups admin seeder test behavior so the module keeps a clear responsibility. */
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSeeder adminSeeder;

    @Test
    @DisplayName("Should seed admin when no admin exists")
    void run_seedsAdminWhenNotExists() {
        ReflectionTestUtils.setField(adminSeeder, "defaultAdminPassword", "Secret@123");
        when(userRepository.findByEmailIgnoreCase("admin@inkwell.dev")).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode("Secret@123")).thenReturn("encodedPassword");

        adminSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("admin@inkwell.dev");
        assertThat(savedUser.getFullName()).isEqualTo("InkWell Admin");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedUser.getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);
    }

    @Test
    @DisplayName("Should not seed admin when admin already exists")
    void run_skipsWhenAdminExists() {
        when(userRepository.findByEmailIgnoreCase("admin@inkwell.dev")).thenReturn(java.util.Optional.of(new User()));

        adminSeeder.run();

        verify(userRepository, never()).save(any(User.class));
    }
}
