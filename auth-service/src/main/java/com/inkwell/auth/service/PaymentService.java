/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.dto.request.CreatePaymentOrderRequest;
import com.inkwell.auth.dto.request.VerifyPaymentRequest;
import com.inkwell.auth.dto.response.PaymentOrderResponse;
import com.inkwell.auth.dto.response.PaymentVerifyResponse;
import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.entity.PaymentOrder;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.PaymentGatewayProvider;
import com.inkwell.auth.enumtype.PaymentStatus;
import com.inkwell.auth.exception.BadRequestException;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.mapper.UserMapper;
import com.inkwell.auth.repository.PaymentOrderRepository;
import com.inkwell.auth.repository.UserRepository;
import com.inkwell.auth.security.GatewayUserPrincipal;
import com.inkwell.auth.security.JwtService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
/* This class groups payment service behavior so the module keeps a clear responsibility. */
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Value("${app.payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Transactional
    // Performs the create order workflow so callers do not duplicate this logic.
    public PaymentOrderResponse createOrder(GatewayUserPrincipal principal, CreatePaymentOrderRequest request) {
        User user = userRepository.findById(principal.userUuid())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String currency = request.currency().trim().toUpperCase();
        BigDecimal amount = request.amount().setScale(2);
        String purpose = request.purpose().trim();
        String description = request.description() == null ? null : request.description().trim();
        PaymentGatewayProvider provider = PaymentGatewayProvider.RAZORPAY;
        String receipt = "inkwell-" + UUID.randomUUID().toString().substring(0, 12);

        PaymentOrder paymentOrder = PaymentOrder.builder()
            .user(user)
            .provider(provider)
            .status(PaymentStatus.CREATED)
            .amount(amount)
            .currency(currency)
            .purpose(purpose)
            .description(description)
            .gatewayReceipt(receipt)
            .build();

        PaymentGatewayClient.RazorpayOrder razorpayOrder = paymentGatewayClient.createRazorpayOrder(
            amount,
            currency,
            receipt,
            Map.of(
                "purpose", purpose,
                "userId", principal.userUuid().toString(),
                "username", principal.username()
            )
        );
        paymentOrder.setGatewayOrderId(razorpayOrder.orderId());
        paymentOrder.setGatewayReceipt(razorpayOrder.receipt());

        return toResponse(paymentOrderRepository.save(paymentOrder));
    }

    /**
     * Verifies a payment and, if the payment is for a subscription (PRO/PLUS),
     * activates the subscription on the user entity. Returns refreshed JWT
     * tokens so the frontend immediately has updated claims (subscriptionTier,
     * subscriptionStatus, role).
     */
    @Transactional
    public PaymentVerifyResponse verifyOrder(GatewayUserPrincipal principal, VerifyPaymentRequest request) {
        PaymentOrder paymentOrder = paymentOrderRepository
            .findByPaymentOrderIdAndUserUserId(request.paymentOrderId(), principal.userUuid())
            .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));

        String normalizedStatus = request.status().trim().toUpperCase();
        if ("FAILED".equals(normalizedStatus)) {
            paymentOrder.setStatus(PaymentStatus.FAILED);
            paymentOrder.setFailureReason("Checkout reported failure");
            PaymentOrder saved = paymentOrderRepository.save(paymentOrder);
            User user = userRepository.findById(principal.userUuid())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return buildVerifyResponse(saved, user);
        }

        if (!paymentOrder.getGatewayOrderId().equals(request.gatewayOrderId())) {
            throw new BadRequestException("Gateway order does not match the stored order");
        }
        if (request.gatewayPaymentId() == null || request.gatewaySignature() == null) {
            throw new BadRequestException("Payment verification details are incomplete");
        }

        String expectedSignature = hmacSha256(paymentOrder.getGatewayOrderId() + "|" + request.gatewayPaymentId(), razorpayKeySecret);
        if (!expectedSignature.equals(request.gatewaySignature())) {
            paymentOrder.setStatus(PaymentStatus.FAILED);
            paymentOrder.setFailureReason("Invalid Razorpay signature");
            paymentOrderRepository.save(paymentOrder);
            throw new BadRequestException("Payment signature verification failed");
        }

        paymentOrder.setGatewayPaymentId(request.gatewayPaymentId());
        paymentOrder.setGatewaySignature(request.gatewaySignature());
        paymentOrder.setStatus(PaymentStatus.VERIFIED);
        paymentOrder.setCompletedAt(LocalDateTime.now());

        // --- Activate subscription based on payment purpose ---
        User user = paymentOrder.getUser();
        if (paymentOrder.getPurpose() != null && (paymentOrder.getPurpose().toUpperCase().contains("PRO") || paymentOrder.getPurpose().toUpperCase().contains("PLUS"))) {
            user.setSubscriptionTier(com.inkwell.auth.enumtype.SubscriptionTier.PRO);
            user.setSubscriptionStatus(com.inkwell.auth.enumtype.SubscriptionStatus.ACTIVE);
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusYears(1);
            user.setSubscriptionEndDate(endDate);
            
            String planName = "Reader Pro";
            String planType = "Reader";
            if (paymentOrder.getPurpose().toUpperCase().contains("PLUS")) {
                user.setRole(com.inkwell.auth.enumtype.Role.AUTHOR);
                planName = "Author Plus";
                planType = "Author";
            }
            userRepository.save(user);

            try {
                emailService.sendPaymentSuccessEmail(
                    new EmailService.PaymentEmailDetails(
                        user.getEmail(),
                        user.getFullName(),
                        planName,
                        planType,
                        paymentOrder.getAmount(),
                        paymentOrder.getCompletedAt(),
                        startDate,
                        endDate
                    )
                );
            } catch (Exception e) {
                // Log and continue, email failure shouldn't fail verification
            }
        }

        PaymentOrder savedOrder = paymentOrderRepository.save(paymentOrder);
        return buildVerifyResponse(savedOrder, user);
    }

    @Transactional(readOnly = true)
    // Performs the get history workflow so callers do not duplicate this logic.
    public List<PaymentOrderResponse> getHistory(GatewayUserPrincipal principal) {
        return paymentOrderRepository.findByUserUserIdOrderByCreatedAtDesc(principal.userUuid())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Build a verify response that includes:
     * - payment order details
     * - a fresh JWT access token (with updated subscription claims)
     * - a new refresh token
     * - the updated user DTO
     */
    private PaymentVerifyResponse buildVerifyResponse(PaymentOrder paymentOrder, User user) {
        // Revoke old refresh tokens and issue fresh credentials
        refreshTokenService.revokeAll(user);
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.createForUser(user).getToken();
        UserResponse userResponse = userMapper.toResponse(user);

        return new PaymentVerifyResponse(
            toResponse(paymentOrder),
            newAccessToken,
            newRefreshToken,
            userResponse
        );
    }

    // Defines to response so related behavior stays grouped in one place.
    private PaymentOrderResponse toResponse(PaymentOrder paymentOrder) {
        return new PaymentOrderResponse(
            paymentOrder.getPaymentOrderId(),
            paymentOrder.getProvider(),
            paymentOrder.getStatus(),
            paymentOrder.getAmount(),
            paymentOrder.getCurrency(),
            paymentOrder.getPurpose(),
            paymentOrder.getDescription(),
            paymentOrder.getGatewayOrderId(),
            paymentOrder.getProvider() == PaymentGatewayProvider.RAZORPAY ? paymentGatewayClient.getRazorpayKeyId() : null,
            paymentOrder.getCreatedAt(),
            paymentOrder.getCompletedAt()
        );
    }

    // Defines hmac sha256 so related behavior stays grouped in one place.
    private String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BadRequestException("Unable to verify payment signature");
        }
    }
}
