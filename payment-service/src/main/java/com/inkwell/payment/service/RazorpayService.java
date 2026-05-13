/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
/* This class groups razorpay service behavior so the module keeps a clear responsibility. */
public class RazorpayService {

    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_CURRENCY = "currency";

    @Value("${app.razorpay.key-id:rzp_test_key}")
    private String keyId;

    @Value("${app.razorpay.key-secret:rzp_test_secret}")
    private String keySecret;

    // Performs the create order workflow so callers do not duplicate this logic.
    public Map<String, Object> createOrder(int amount) throws RazorpayException {
        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
        
        JSONObject orderRequest = new JSONObject();
        orderRequest.put(KEY_AMOUNT, amount * 100);
        orderRequest.put(KEY_CURRENCY, "INR");
        orderRequest.put("receipt", "order_rcptid_" + UUID.randomUUID().toString().substring(0, 8));

        Order order = razorpay.orders.create(orderRequest);
        
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.get("id"));
        response.put(KEY_AMOUNT, order.get(KEY_AMOUNT));
        response.put(KEY_CURRENCY, order.get(KEY_CURRENCY));
        response.put("keyId", keyId);
        
        return response;
    }
}
