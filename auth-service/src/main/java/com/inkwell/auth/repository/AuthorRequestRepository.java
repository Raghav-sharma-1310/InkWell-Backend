/*
 * Codex documentation pass: this source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.repository;

import com.inkwell.auth.entity.AuthorRequest;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/* This interface groups author request repository behavior so the module keeps a clear responsibility. */
public interface AuthorRequestRepository extends JpaRepository<AuthorRequest, UUID> {

    Optional<AuthorRequest> findByUser(User user);

    Optional<AuthorRequest> findByUserAndStatusIn(User user, List<RequestStatus> statuses);

    boolean existsByUserAndStatus(User user, RequestStatus status);

    List<AuthorRequest> findAllByOrderByRequestedAtDesc();

    List<AuthorRequest> findByStatusOrderByRequestedAtDesc(RequestStatus status);
}
