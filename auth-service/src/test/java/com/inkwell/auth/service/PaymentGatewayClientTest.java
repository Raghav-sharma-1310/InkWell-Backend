/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkwell.auth.exception.BadRequestException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

/* This class groups payment gateway client test behavior so the module keeps a clear responsibility. */
class PaymentGatewayClientTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final PaymentGatewayClient client = new PaymentGatewayClient(new ObjectMapper(), httpClient);

    @BeforeEach
    void setBlankCredentials() {
        ReflectionTestUtils.setField(client, "razorpayKeyId", "");
        ReflectionTestUtils.setField(client, "razorpayKeySecret", "");
    }

    @Test
    void configurationRequiresBothRazorpayCredentials() {
        assertThat(client.razorpayConfigured()).isFalse();

        ReflectionTestUtils.setField(client, "razorpayKeyId", "key_id");
        assertThat(client.razorpayConfigured()).isFalse();

        ReflectionTestUtils.setField(client, "razorpayKeySecret", "key_secret");
        assertThat(client.razorpayConfigured()).isTrue();
        assertThat(client.getRazorpayKeyId()).isEqualTo("key_id");
    }

    @Test
    void createRazorpayOrderSendsAuthenticatedRequest() throws IOException, InterruptedException {
        configureCredentials();
        HttpResponse<String> response = response(200, "{\"id\":\"order_123\",\"receipt\":\"receipt_123\"}");
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        when(httpClient.send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(response);

        PaymentGatewayClient.RazorpayOrder order = client.createRazorpayOrder(
            BigDecimal.valueOf(149),
            "INR",
            "receipt_123",
            Map.of("purpose", "Reader Pro")
        );

        HttpRequest request = requestCaptor.getValue();
        String expectedAuthorization = "Basic " + Base64.getEncoder()
            .encodeToString("key_id:key_secret".getBytes(StandardCharsets.UTF_8));
        assertThat(order.orderId()).isEqualTo("order_123");
        assertThat(order.receipt()).isEqualTo("receipt_123");
        assertThat(request.uri()).hasToString("https://api.razorpay.com/v1/orders");
        assertThat(request.headers().firstValue("Authorization")).contains(expectedAuthorization);
        assertThat(request.headers().firstValue("Content-Type")).contains("application/json");
    }

    @Test
    void createRazorpayOrderRejectsGatewayError() throws IOException, InterruptedException {
        configureCredentials();
        HttpResponse<String> response = response(400, "{\"error\":\"bad request\"}");
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(response);
        BigDecimal amount = BigDecimal.TEN;
        Map<String, String> notes = Map.of();

        assertThatThrownBy(() -> client.createRazorpayOrder(amount, "INR", "receipt", notes))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Razorpay order creation failed");
    }

    @Test
    void createRazorpayOrderWrapsIoFailure() throws IOException, InterruptedException {
        configureCredentials();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenThrow(new IOException("network"));
        BigDecimal amount = BigDecimal.TEN;
        Map<String, String> notes = Map.of();

        assertThatThrownBy(() -> client.createRazorpayOrder(amount, "INR", "receipt", notes))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Unable to create Razorpay order: network");
    }

    @Test
    void createRazorpayOrderRestoresInterruptedFlag() throws IOException, InterruptedException {
        configureCredentials();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenThrow(new InterruptedException("interrupted"));
        BigDecimal amount = BigDecimal.TEN;
        Map<String, String> notes = Map.of();

        assertThatThrownBy(() -> client.createRazorpayOrder(amount, "INR", "receipt", notes))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Unable to create Razorpay order: interrupted");
        assertThat(Thread.interrupted()).isTrue();
    }

    // Provides configure credentials wiring so the framework can apply the expected runtime behavior.
    private void configureCredentials() {
        ReflectionTestUtils.setField(client, "razorpayKeyId", "key_id");
        ReflectionTestUtils.setField(client, "razorpayKeySecret", "key_secret");
    }

    @SuppressWarnings("unchecked")
    // Defines response so related behavior stays grouped in one place.
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
