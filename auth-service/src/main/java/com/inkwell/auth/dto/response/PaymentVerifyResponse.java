/*
 * Codex documentation pass: this source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.response;

/**
 * Response returned after successful payment verification.
 * Includes the payment order details AND refreshed auth credentials,
 * so the frontend can immediately update both payment history and user/session state.
 */
public record PaymentVerifyResponse(
    PaymentOrderResponse paymentOrder,
    String accessToken,
    String refreshToken,
    UserResponse user
) {
}
