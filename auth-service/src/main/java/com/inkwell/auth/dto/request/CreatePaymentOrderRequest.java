/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/* This record groups create payment order request behavior so the module keeps a clear responsibility. */
public record CreatePaymentOrderRequest(
    @DecimalMin(value = "1.00", message = "Minimum payment amount is 1.00")
    BigDecimal amount,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotBlank @Size(min = 3, max = 60) String purpose,
    @Size(max = 255) String description
) {
}
