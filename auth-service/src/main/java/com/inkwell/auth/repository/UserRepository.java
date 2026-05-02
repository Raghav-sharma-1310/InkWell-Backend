/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.repository;

import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups user repository behavior so the module keeps a clear responsibility. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    List<User> findByRole(Role role);

    List<User> findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String fullName, String username);
}
