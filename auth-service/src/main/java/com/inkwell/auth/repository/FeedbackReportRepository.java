/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.repository;

import com.inkwell.auth.entity.FeedbackReport;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.FeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/* This interface groups feedback report repository behavior so the module keeps a clear responsibility. */
public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, UUID> {

    List<FeedbackReport> findByUserOrderByCreatedAtDesc(User user);

    Optional<FeedbackReport> findByIdAndUser(UUID id, User user);

    List<FeedbackReport> findAllByOrderByCreatedAtDesc();

    List<FeedbackReport> findByStatusOrderByCreatedAtDesc(FeedbackStatus status);
}
