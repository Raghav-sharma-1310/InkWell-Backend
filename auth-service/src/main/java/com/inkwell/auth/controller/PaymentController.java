/*
 * this source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.request.CreatePaymentOrderRequest;
import com.inkwell.auth.dto.request.VerifyPaymentRequest;
import com.inkwell.auth.dto.response.PaymentOrderResponse;
import com.inkwell.auth.dto.response.PaymentVerifyResponse;
import com.inkwell.auth.service.PaymentService;
import com.inkwell.auth.util.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/payments")
@RequiredArgsConstructor
/* This class groups payment controller behavior so the module keeps a clear responsibility. */
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders")
    // Performs the create order workflow so callers do not duplicate this logic.
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(@Valid @RequestBody CreatePaymentOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Payment order created", paymentService.createOrder(SecurityUtils.currentPrincipal(), request)));
    }

    @PostMapping("/verify")
    // Performs the verify payment workflow so callers do not duplicate this logic.
    public ResponseEntity<ApiResponse<PaymentVerifyResponse>> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Payment verified", paymentService.verifyOrder(SecurityUtils.currentPrincipal(), request)));
    }

    @GetMapping("/history")
    // Defines history so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<List<PaymentOrderResponse>>> history() {
        return ResponseEntity.ok(ApiResponse.of("Payment history fetched", paymentService.getHistory(SecurityUtils.currentPrincipal())));
    }
}
