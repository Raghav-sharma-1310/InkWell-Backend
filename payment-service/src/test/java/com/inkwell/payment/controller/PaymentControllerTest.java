/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.payment.controller;

import com.inkwell.payment.service.RazorpayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
/* This class groups payment controller test behavior so the module keeps a clear responsibility. */
class PaymentControllerTest {

    @Mock
    private RazorpayService razorpayService;

    @InjectMocks
    private PaymentController paymentController;


    @Test
    @DisplayName("Should create order successfully")
    void createOrderSuccess() throws Exception {
        java.util.Map<String, Object> mockResponse = new java.util.HashMap<>();
        mockResponse.put("orderId", "order_123");
        org.mockito.Mockito.when(razorpayService.createOrder(500)).thenReturn(mockResponse);

        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("amount", 500);
        org.springframework.http.ResponseEntity<java.util.Map<String, Object>> response = paymentController.createOrder(request);

        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals("order_123", response.getBody().get("orderId"));
    }
}
