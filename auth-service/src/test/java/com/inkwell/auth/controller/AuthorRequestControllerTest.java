/*
 * Codex documentation pass: this source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.request.AdminRemarkRequest;
import com.inkwell.auth.dto.response.AuthorRequestResponse;
import com.inkwell.auth.enumtype.RequestStatus;
import com.inkwell.auth.service.AuthorRequestService;
import com.inkwell.auth.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/* This class groups author request controller test behavior so the module keeps a clear responsibility. */
class AuthorRequestControllerTest {

    @Mock
    private AuthorRequestService authorRequestService;

    @InjectMocks
    private AuthorRequestController authorRequestController;

    @InjectMocks
    private AdminAuthorRequestController adminAuthorRequestController;

    private UUID requestId;
    private UUID userId;
    private AuthorRequestResponse response;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        userId = UUID.randomUUID();
        response = new AuthorRequestResponse(requestId, userId, "user", "test@test.com", "User Full Name", RequestStatus.PENDING, null, LocalDateTime.now(), LocalDateTime.now());

        com.inkwell.auth.security.GatewayUserPrincipal principal = new com.inkwell.auth.security.GatewayUserPrincipal(
            userId.toString(), "admin", "ADMIN", "admin@test.com");
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentPrincipal).thenReturn(principal);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Test
    void testSubmitRequest() {
        when(authorRequestService.submitRequest(userId)).thenReturn(response);
        var res = authorRequestController.submitRequest();
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }

    @Test
    void testGetStatus() {
        when(authorRequestService.getRequestStatus(userId)).thenReturn(response);
        var res = authorRequestController.getStatus();
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }

    @Test
    void testAdminGetAll() {
        when(authorRequestService.getAllRequests()).thenReturn(List.of(response));
        var res = adminAuthorRequestController.getAllRequests();
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }

    @Test
    void testAdminApprove() {
        when(authorRequestService.approveRequest(eq(requestId), any())).thenReturn(response);
        var res = adminAuthorRequestController.approve(requestId, new AdminRemarkRequest("ok"));
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }

    @Test
    void testAdminReject() {
        when(authorRequestService.rejectRequest(eq(requestId), any())).thenReturn(response);
        var res = adminAuthorRequestController.reject(requestId, new AdminRemarkRequest("no"));
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }
}
