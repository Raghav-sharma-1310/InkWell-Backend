/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.payment.service;

import com.razorpay.RazorpayException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
/* This class groups razorpay service test behavior so the module keeps a clear responsibility. */
class RazorpayServiceTest {

    @InjectMocks private RazorpayService razorpayService;

    @Test
    @DisplayName("Should fail gracefully with invalid credentials")
    void createOrderInvalidCredentials() {
        ReflectionTestUtils.setField(razorpayService, "keyId", "rzp_test_invalid");
        ReflectionTestUtils.setField(razorpayService, "keySecret", "test_secret_invalid");

        assertThatThrownBy(() -> razorpayService.createOrder(500))
                .isInstanceOf(RazorpayException.class);
    }

    @Test
    @DisplayName("Should use configured key ID")
    void usesConfiguredKeyId() {
        ReflectionTestUtils.setField(razorpayService, "keyId", "rzp_test_mykey");
        ReflectionTestUtils.setField(razorpayService, "keySecret", "test_secret");

        // We can verify the field was set correctly
        String keyId = (String) ReflectionTestUtils.getField(razorpayService, "keyId");
        assertThat(keyId).isEqualTo("rzp_test_mykey");
    }

    @Test
    @DisplayName("Should create order successfully")
    void createOrderSuccess() throws RazorpayException {
        ReflectionTestUtils.setField(razorpayService, "keyId", "test");
        ReflectionTestUtils.setField(razorpayService, "keySecret", "test");
        
        com.razorpay.Order mockOrder = org.mockito.Mockito.mock(com.razorpay.Order.class);
        org.mockito.Mockito.when(mockOrder.get("id")).thenReturn("order_123");
        org.mockito.Mockito.when(mockOrder.get("amount")).thenReturn(50000);
        org.mockito.Mockito.when(mockOrder.get("currency")).thenReturn("INR");

        try (org.mockito.MockedConstruction<com.razorpay.RazorpayClient> mocked = org.mockito.Mockito.mockConstruction(com.razorpay.RazorpayClient.class,
                (mock, context) -> {
                    com.razorpay.OrderClient mockOrderClient = org.mockito.Mockito.mock(com.razorpay.OrderClient.class);
                    org.mockito.Mockito.when(mockOrderClient.create(org.mockito.ArgumentMatchers.any())).thenReturn(mockOrder);
                    mock.orders = mockOrderClient;
                })) {
            
            java.util.Map<String, Object> result = razorpayService.createOrder(500);
            
            assertThat(result).containsEntry("orderId", "order_123")
                              .containsEntry("amount", 50000)
                              .containsEntry("currency", "INR")
                              .containsEntry("keyId", "test");
        }
    }
}
