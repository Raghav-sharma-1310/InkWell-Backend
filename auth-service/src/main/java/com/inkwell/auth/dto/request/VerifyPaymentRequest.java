/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/* This record groups verify payment request behavior so the module keeps a clear responsibility. */
public record VerifyPaymentRequest(
    UUID paymentOrderId,
    String gatewayOrderId,
    String gatewayPaymentId,
    String gatewaySignature,
    @NotBlank String status
) {
}
