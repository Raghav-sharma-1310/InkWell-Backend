/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.response;

import com.inkwell.auth.enumtype.PaymentGatewayProvider;
import com.inkwell.auth.enumtype.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/* This record groups payment order response behavior so the module keeps a clear responsibility. */
public record PaymentOrderResponse(
    UUID paymentOrderId,
    PaymentGatewayProvider provider,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String purpose,
    String description,
    String gatewayOrderId,
    String gatewayPublicKey,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {
}
