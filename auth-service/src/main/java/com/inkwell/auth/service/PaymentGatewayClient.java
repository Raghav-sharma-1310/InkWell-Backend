/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkwell.auth.exception.BadRequestException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
/* This class groups payment gateway client behavior so the module keeps a clear responsibility. */
public class PaymentGatewayClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.payment.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${app.payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Autowired
    // Defines payment gateway client so related behavior stays grouped in one place.
    public PaymentGatewayClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newHttpClient());
    }

    PaymentGatewayClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    // Provides razorpay configured wiring so the framework can apply the expected runtime behavior.
    public boolean razorpayConfigured() {
        return !razorpayKeyId.isBlank() && !razorpayKeySecret.isBlank();
    }

    // Performs the get razorpay key id workflow so callers do not duplicate this logic.
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    // Performs the create razorpay order workflow so callers do not duplicate this logic.
    public RazorpayOrder createRazorpayOrder(BigDecimal amount, String currency, String receipt, Map<String, String> notes) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "amount", amount.movePointRight(2).intValueExact(),
                "currency", currency,
                "receipt", receipt,
                "notes", notes
            ));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com/v1/orders"))
                .header("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Razorpay order creation failed: " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return new RazorpayOrder(root.path("id").asText(), root.path("receipt").asText());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to create Razorpay order: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to create Razorpay order: " + ex.getMessage());
        }
    }

    /* This record groups razorpay order behavior so the module keeps a clear responsibility. */
    public record RazorpayOrder(String orderId, String receipt) {
    }
}
