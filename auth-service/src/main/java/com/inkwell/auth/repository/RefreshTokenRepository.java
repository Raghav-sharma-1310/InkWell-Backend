/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.repository;

import com.inkwell.auth.entity.RefreshToken;
import com.inkwell.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups refresh token repository behavior so the module keeps a clear responsibility. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser(User user);

    void deleteByToken(String token);

    void deleteByUser(User user);
}
