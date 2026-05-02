/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.dto.request.CreatePaymentOrderRequest;
import com.inkwell.auth.dto.request.VerifyPaymentRequest;
import com.inkwell.auth.dto.response.PaymentOrderResponse;
import com.inkwell.auth.dto.response.PaymentVerifyResponse;
import com.inkwell.auth.entity.PaymentOrder;
import com.inkwell.auth.entity.RefreshToken;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.AuthProvider;
import com.inkwell.auth.enumtype.PaymentGatewayProvider;
import com.inkwell.auth.enumtype.PaymentStatus;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.enumtype.SubscriptionStatus;
import com.inkwell.auth.enumtype.SubscriptionTier;
import com.inkwell.auth.mapper.UserMapper;
import com.inkwell.auth.repository.PaymentOrderRepository;
import com.inkwell.auth.repository.UserRepository;
import com.inkwell.auth.security.GatewayUserPrincipal;
import com.inkwell.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups payment service test behavior so the module keeps a clear responsibility. */
class PaymentServiceTest {

    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentGatewayClient paymentGatewayClient;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserMapper userMapper;
    @Mock private EmailService emailService;

    @InjectMocks private PaymentService paymentService;

    private User testUser;
    private GatewayUserPrincipal principal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .userId(userId)
                .username("test")
                .email("test@inkwell.com")
                .fullName("Test User")
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .subscriptionTier(SubscriptionTier.FREE)
                .subscriptionStatus(SubscriptionStatus.CANCELLED)
                .createdAt(LocalDateTime.now())
                .build();
        principal = new GatewayUserPrincipal(userId.toString(), "test", "test@inkwell.com", "READER");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret");
    }

    @Test
    @DisplayName("Should create payment order")
    void createOrder() {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(BigDecimal.TEN, "INR", "PRO Plan", "Test payment");
        PaymentGatewayClient.RazorpayOrder razorpayOrder = new PaymentGatewayClient.RazorpayOrder("order_123", "receipt_123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(paymentGatewayClient.createRazorpayOrder(any(), any(), any(), any())).thenReturn(razorpayOrder);
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentOrderResponse response = paymentService.createOrder(principal, request);

        assertThat(response).isNotNull();
        assertThat(response.gatewayOrderId()).isEqualTo("order_123");
        verify(paymentOrderRepository).save(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("Should throw when creating payment order for missing user")
    void createOrderMissingUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(BigDecimal.TEN, "INR", "PRO Plan", null);

        assertThatThrownBy(() -> paymentService.createOrder(principal, request))
                .isInstanceOf(com.inkwell.auth.exception.ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Should mark failed payment and return refreshed credentials")
    void verifyFailedPayment() {
        PaymentOrder order = paymentOrder("Reader Pro");
        when(paymentOrderRepository.findByPaymentOrderIdAndUserUserId(order.getPaymentOrderId(), userId)).thenReturn(Optional.of(order));
        when(paymentOrderRepository.save(order)).thenReturn(order);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        stubTokenResponse();

        PaymentVerifyResponse response = paymentService.verifyOrder(principal,
                new VerifyPaymentRequest(order.getPaymentOrderId(), "order_123", null, null, " failed "));

        assertThat(response.paymentOrder().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getFailureReason()).isEqualTo("Checkout reported failure");
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("Should verify reader pro payment and activate subscription")
    void verifyReaderProPayment() {
        PaymentOrder order = paymentOrder("InkWell Reader Pro");
        String signature = signature(order.getGatewayOrderId(), "pay_123");
        when(paymentOrderRepository.findByPaymentOrderIdAndUserUserId(order.getPaymentOrderId(), userId)).thenReturn(Optional.of(order));
        when(paymentOrderRepository.save(order)).thenReturn(order);
        when(userRepository.save(testUser)).thenReturn(testUser);
        stubTokenResponse();

        PaymentVerifyResponse response = paymentService.verifyOrder(principal,
                new VerifyPaymentRequest(order.getPaymentOrderId(), "order_123", "pay_123", signature, "success"));

        assertThat(response.paymentOrder().status()).isEqualTo(PaymentStatus.VERIFIED);
        assertThat(testUser.getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(testUser.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(testUser.getRole()).isEqualTo(Role.READER);
        verify(emailService).sendPaymentSuccessEmail(any());
    }

    @Test
    @DisplayName("Should verify author plus payment and ignore email failures")
    void verifyAuthorPlusPaymentEmailFailureIgnored() {
        PaymentOrder order = paymentOrder("InkWell Author Plus");
        String signature = signature(order.getGatewayOrderId(), "pay_123");
        doThrow(new RuntimeException("smtp")).when(emailService).sendPaymentSuccessEmail(any());
        when(paymentOrderRepository.findByPaymentOrderIdAndUserUserId(order.getPaymentOrderId(), userId)).thenReturn(Optional.of(order));
        when(paymentOrderRepository.save(order)).thenReturn(order);
        when(userRepository.save(testUser)).thenReturn(testUser);
        stubTokenResponse();

        PaymentVerifyResponse response = paymentService.verifyOrder(principal,
                new VerifyPaymentRequest(order.getPaymentOrderId(), "order_123", "pay_123", signature, "success"));

        assertThat(response.paymentOrder().status()).isEqualTo(PaymentStatus.VERIFIED);
        assertThat(testUser.getRole()).isEqualTo(Role.AUTHOR);
    }

    @Test
    @DisplayName("Should reject mismatched gateway order")
    void verifyMismatchedGatewayOrder() {
        PaymentOrder order = paymentOrder("Reader Pro");
        when(paymentOrderRepository.findByPaymentOrderIdAndUserUserId(order.getPaymentOrderId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.verifyOrder(principal,
                new VerifyPaymentRequest(order.getPaymentOrderId(), "other", "pay_123", "sig", "success")))
                .isInstanceOf(com.inkwell.auth.exception.BadRequestException.class)
                .hasMessage("Gateway order does not match the stored order");
    }

    @Test
    @DisplayName("Should reject missing verification fields")
    void verifyMissingDetails() {
        PaymentOrder order = paymentOrder("Reader Pro");
        when(paymentOrderRepository.findByPaymentOrderIdAndUserUserId(order.getPaymentOrderId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.verifyOrder(principal,
                new VerifyPaymentRequest(order.getPaymentOrderId(), "order_123", null, null, "success")))
                .isInstanceOf(com.inkwell.auth.exception.BadRequestException.class)
                .hasMessage("Payment verification details are incomplete");
    }

    @Test
    @DisplayName("Should reject invalid signature")
    void verifyInvalidSignature() {
        PaymentOrder order = paymentOrder("Reader Pro");
        when(paymentOrderRepository.findByPaymentOrderIdAndUserUserId(order.getPaymentOrderId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.verifyOrder(principal,
                new VerifyPaymentRequest(order.getPaymentOrderId(), "order_123", "pay_123", "bad-signature", "success")))
                .isInstanceOf(com.inkwell.auth.exception.BadRequestException.class)
                .hasMessage("Payment signature verification failed");
        assertThat(order.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getFailureReason()).isEqualTo("Invalid Razorpay signature");
    }

    @Test
    @DisplayName("Should fetch payment history")
    void getHistory() {
        PaymentOrder order = paymentOrder("Reader Pro");
        when(paymentOrderRepository.findByUserUserIdOrderByCreatedAtDesc(userId)).thenReturn(java.util.List.of(order));

        assertThat(paymentService.getHistory(principal)).hasSize(1);
    }

    // Defines payment order so related behavior stays grouped in one place.
    private PaymentOrder paymentOrder(String purpose) {
        return PaymentOrder.builder()
                .paymentOrderId(UUID.randomUUID())
                .user(testUser)
                .provider(PaymentGatewayProvider.RAZORPAY)
                .status(PaymentStatus.CREATED)
                .amount(BigDecimal.valueOf(149))
                .currency("INR")
                .purpose(purpose)
                .description("Subscription")
                .gatewayOrderId("order_123")
                .gatewayReceipt("receipt_123")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // Defines stub token response so related behavior stays grouped in one place.
    private void stubTokenResponse() {
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(refreshTokenService.createForUser(testUser)).thenReturn(RefreshToken.builder().token("refresh-token").user(testUser).build());
        when(userMapper.toResponse(testUser)).thenReturn(new com.inkwell.auth.dto.response.UserResponse(
                userId,
                "test",
                "test@inkwell.com",
                "Test User",
                testUser.getRole(),
                null,
                null,
                null,
                AuthProvider.LOCAL,
                true,
                testUser.getCreatedAt(),
                testUser.getSubscriptionTier(),
                testUser.getSubscriptionStatus(),
                testUser.getSubscriptionEndDate()
        ));
    }

    // Defines signature so related behavior stays grouped in one place.
    private String signature(String orderId, String paymentId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("test_secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
