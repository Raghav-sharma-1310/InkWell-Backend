/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.payment.controller;

import com.razorpay.RazorpayException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
/* This class groups payment controller behavior so the module keeps a clear responsibility. */
public class PaymentController {

    private final com.inkwell.payment.service.RazorpayService razorpayService;

    // Handles payment controller requests so the UI can call this feature through a stable endpoint.
    public PaymentController(com.inkwell.payment.service.RazorpayService razorpayService) {
        this.razorpayService = razorpayService;
    }

    @PostMapping("/create-order")
    // Performs the create order workflow so callers do not duplicate this logic.
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> data) throws RazorpayException {
        int amount = (int) data.get("amount");
        return ResponseEntity.ok(razorpayService.createOrder(amount));
    }
}
