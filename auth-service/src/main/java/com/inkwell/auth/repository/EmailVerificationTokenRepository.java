/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.repository;

import com.inkwell.auth.entity.EmailVerificationToken;
import com.inkwell.auth.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups email verification token repository behavior so the module keeps a clear responsibility. */
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);
    Optional<EmailVerificationToken> findByUserAndUsedFalse(User user);
    List<EmailVerificationToken> findAllByUser(User user);
}
