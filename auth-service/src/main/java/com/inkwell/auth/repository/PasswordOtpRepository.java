/*
 * Codex documentation pass: this source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.repository;

import com.inkwell.auth.entity.PasswordOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups password otp repository behavior so the module keeps a clear responsibility. */
public interface PasswordOtpRepository extends JpaRepository<PasswordOtp, Long> {

    Optional<PasswordOtp> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);
}
