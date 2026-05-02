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
import com.inkwell.auth.repository.AuthorRequestRepository;
import com.inkwell.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups author request service test behavior so the module keeps a clear responsibility. */
class AuthorRequestServiceTest {

    @Mock
    private AuthorRequestRepository authorRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthorRequestService authorRequestService;

    private UUID userId;
    private UUID requestId;
    private User mockUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        requestId = UUID.randomUUID();

        mockUser = User.builder()
                .userId(userId)
                .email("user@test.com")
                .role(Role.READER)
                .build();
    }

    @Test
    void testSubmitRequest_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(authorRequestRepository.existsByUserAndStatus(mockUser, RequestStatus.PENDING)).thenReturn(false);

        AuthorRequest savedRequest = AuthorRequest.builder()
                .id(requestId)
                .user(mockUser)
                .status(RequestStatus.PENDING)
                .build();
        when(authorRequestRepository.save(any(AuthorRequest.class))).thenReturn(savedRequest);

        AuthorRequestResponse response = authorRequestService.submitRequest(userId);

        assertNotNull(response);
        assertEquals(RequestStatus.PENDING, response.status());
        verify(authorRequestRepository).save(any(AuthorRequest.class));
    }

    @Test
    void testSubmitRequest_AlreadyPending() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(authorRequestRepository.existsByUserAndStatus(mockUser, RequestStatus.PENDING)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authorRequestService.submitRequest(userId));
        verify(authorRequestRepository, never()).save(any(AuthorRequest.class));
    }

    @Test
    void testGetRequestStatus_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        AuthorRequest request = AuthorRequest.builder()
                .id(requestId)
                .user(mockUser)
                .status(RequestStatus.PENDING)
                .build();
        when(authorRequestRepository.findByUser(mockUser)).thenReturn(Optional.of(request));

        AuthorRequestResponse response = authorRequestService.getRequestStatus(userId);

        assertNotNull(response);
        assertEquals(RequestStatus.PENDING, response.status());
    }

    @Test
    void testGetRequestStatus_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(authorRequestRepository.findByUser(mockUser)).thenReturn(Optional.empty());

        AuthorRequestResponse response = authorRequestService.getRequestStatus(userId);

        assertNull(response);
    }

    @Test
    void testGetAllRequests() {
        when(authorRequestRepository.findAllByOrderByRequestedAtDesc()).thenReturn(List.of());

        List<AuthorRequestResponse> requests = authorRequestService.getAllRequests();

        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    void testApproveRequest() {
        AuthorRequest request = AuthorRequest.builder()
                .id(requestId)
                .user(mockUser)
                .status(RequestStatus.PENDING)
                .build();
        when(authorRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(authorRequestRepository.save(any(AuthorRequest.class))).thenReturn(request);

        AuthorRequestResponse response = authorRequestService.approveRequest(requestId, "Approved");

        assertNotNull(response);
        assertEquals(RequestStatus.APPROVED, response.status());
        assertEquals("Approved", response.adminRemarks());
        assertEquals(Role.AUTHOR, mockUser.getRole());
        verify(auditLogService).logAction(any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testRejectRequest() {
        AuthorRequest request = AuthorRequest.builder()
                .id(requestId)
                .user(mockUser)
                .status(RequestStatus.PENDING)
                .build();
        when(authorRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(authorRequestRepository.save(any(AuthorRequest.class))).thenReturn(request);

        AuthorRequestResponse response = authorRequestService.rejectRequest(requestId, "Rejected");

        assertNotNull(response);
        assertEquals(RequestStatus.REJECTED, response.status());
        assertEquals("Rejected", response.adminRemarks());
    }
}
