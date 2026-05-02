/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.dto.response.AuthorRequestResponse;
import com.inkwell.auth.entity.AuthorRequest;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.RequestStatus;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.exception.BadRequestException;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.repository.AuthorRequestRepository;
import com.inkwell.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups author request service behavior so the module keeps a clear responsibility. */
public class AuthorRequestService {

    private final AuthorRequestRepository authorRequestRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    // Defines submit request so related behavior stays grouped in one place.
    public AuthorRequestResponse submitRequest(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.READER) {
            throw new BadRequestException("Only readers can request to become an author");
        }

        // Prevent duplicate pending requests
        if (authorRequestRepository.existsByUserAndStatus(user, RequestStatus.PENDING)) {
            throw new BadRequestException("You already have a pending author request");
        }

        AuthorRequest request = AuthorRequest.builder()
            .user(user)
            .status(RequestStatus.PENDING)
            .build();

        AuthorRequest saved = authorRequestRepository.save(request);
        log.info("Author request submitted by user {}", user.getEmail());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    // Performs the get request status workflow so callers do not duplicate this logic.
    public AuthorRequestResponse getRequestStatus(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AuthorRequest request = authorRequestRepository.findByUser(user)
            .orElse(null);

        if (request == null) {
            return null;
        }
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    // Performs the get all requests workflow so callers do not duplicate this logic.
    public List<AuthorRequestResponse> getAllRequests() {
        return authorRequestRepository.findAllByOrderByRequestedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    // Defines approve request so related behavior stays grouped in one place.
    public AuthorRequestResponse approveRequest(UUID requestId, String remarks) {
        AuthorRequest request = authorRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Author request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be approved");
        }

        // Update request
        request.setStatus(RequestStatus.APPROVED);
        request.setAdminRemarks(remarks);

        // Promote user to AUTHOR
        User user = request.getUser();
        String oldRole = user.getRole().name();
        user.setRole(Role.AUTHOR);
        userRepository.save(user);

        log.info("Author request {} approved. User {} promoted to AUTHOR", requestId, user.getEmail());

        auditLogService.logAction(
            null,
            "System/Admin",
            "AUTHOR_REQUEST_APPROVED",
            "USER",
            user.getUserId().toString(),
            "Role changed from " + oldRole + " to AUTHOR via author request"
        );

        return toResponse(authorRequestRepository.save(request));
    }

    @Transactional
    // Defines reject request so related behavior stays grouped in one place.
    public AuthorRequestResponse rejectRequest(UUID requestId, String remarks) {
        AuthorRequest request = authorRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Author request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be rejected");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setAdminRemarks(remarks);

        log.info("Author request {} rejected for user {}", requestId, request.getUser().getEmail());

        return toResponse(authorRequestRepository.save(request));
    }

    // Defines to response so related behavior stays grouped in one place.
    private AuthorRequestResponse toResponse(AuthorRequest request) {
        User user = request.getUser();
        return new AuthorRequestResponse(
            request.getId(),
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            request.getStatus(),
            request.getAdminRemarks(),
            request.getRequestedAt(),
            request.getUpdatedAt()
        );
    }
}
