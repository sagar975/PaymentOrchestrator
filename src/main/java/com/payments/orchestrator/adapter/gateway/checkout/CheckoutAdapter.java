package com.payments.orchestrator.adapter.gateway.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orchestrator.config.CheckoutConfig;
import com.payments.orchestrator.domain.exception.GatewayException;
import com.payments.orchestrator.domain.model.GatewayResponse;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.port.GatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Checkout.com gateway adapter implementing GatewayPort.
 * <p>
 * Covers UAE, MENA, UK, EU, Asia Pacific.
 * Acts as global fallback when Stripe fails.
 * <p>
 * Checkout.com NAS platform uses Bearer token authentication
 * and amount in smallest currency unit (cents/fils).
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class CheckoutAdapter implements GatewayPort {

    private static final String GATEWAY_NAME = "checkout";
    private static final MediaType JSON =
            MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final CheckoutConfig checkoutConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String getGatewayName() {
        return GATEWAY_NAME;
    }

    /**
     * Authorizes a payment via Checkout.com.
     * Creates a payment request with manual capture.
     *
     * @param transaction the transaction to authorize
     * @return GatewayResponse with payment details
     * @throws GatewayException if Checkout.com rejects or unreachable
     */
    @Override
    public GatewayResponse authorize(Transaction transaction) {
        log.info("Authorizing transaction via Checkout.com. txnId={}, amount={}",
                transaction.getId(), transaction.getAmount());

        try {
            long amountInSmallestUnit = transaction.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            // Test card source — required by Checkout.com
            Map<String, Object> source = new HashMap<>();
            source.put("type", "card");
            source.put("number", "4242424242424242");
            source.put("expiry_month", 12);
            source.put("expiry_year", 2030);
            source.put("cvv", "100");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("source", source);
            requestBody.put("amount", amountInSmallestUnit);
            requestBody.put("currency",
                    transaction.getCurrency().toUpperCase());
            requestBody.put("reference",
                    transaction.getId().toString());
            requestBody.put("capture", false);
            requestBody.put("processing_channel_id",
                    checkoutConfig.getProcessingChannelId());
            requestBody.put("metadata", Map.of(
                    "transactionId", transaction.getId().toString(),
                    "merchantId", transaction.getMerchantId(),
                    "idempotencyKey", transaction.getIdempotencyKey()));

            String json = objectMapper.writeValueAsString(requestBody);

            log.info("Checkout.com request. url={}, body={}",
                    checkoutConfig.getBaseUrl() + "/payments", json);

            Request request = new Request.Builder()
                    .url(checkoutConfig.getBaseUrl() + "/payments")
                    .addHeader("Authorization",
                            "Bearer " + checkoutConfig.getSecretKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "";

                log.info("Checkout.com raw response. code={}, body={}",
                        response.code(), responseBody);

                if (response.isSuccessful()) {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    String paymentId = jsonNode.path("id").asText();
                    String status = jsonNode.path("status").asText();

                    log.info("Checkout.com payment created. paymentId={}, " +
                                    "status={}, txnId={}",
                            paymentId, status, transaction.getId());

                    return GatewayResponse.success(paymentId, responseBody);

                } else {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    String errorCode = jsonNode.path("error_codes")
                            .path(0).asText("CHECKOUT_ERROR");
                    String errorMessage = jsonNode
                            .path("error_message")
                            .asText("Payment failed");

                    log.warn("Checkout.com rejected. code={}, message={}, txnId={}",
                            errorCode, errorMessage, transaction.getId());

                    return GatewayResponse.failure(
                            errorCode, errorMessage, responseBody);
                }
            }

        } catch (IOException e) {
            log.error("Checkout.com network error. txnId={}",
                    transaction.getId(), e);
            throw new GatewayException(
                    GATEWAY_NAME,
                    "GATEWAY_TIMEOUT",
                    "Checkout.com unreachable: " + e.getMessage(),
                    true,
                    e);
        } catch (Exception e) {
            log.error("Checkout.com unexpected error. txnId={}",
                    transaction.getId(), e);
            throw new GatewayException(
                    GATEWAY_NAME,
                    "CHECKOUT_ERROR",
                    "Unexpected error: " + e.getMessage(),
                    false,
                    e);
        }
    }

    /**
     * Captures a Checkout.com payment.
     * POST /payments/{id}/captures
     */
    @Override
    public GatewayResponse capture(Transaction transaction) {
        log.info("Capturing Checkout.com payment. txnId={}",
                transaction.getId());

        try {
            long amountInSmallestUnit = transaction.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", amountInSmallestUnit);
            requestBody.put("reference",
                    transaction.getId().toString());

            String json = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(checkoutConfig.getBaseUrl() + "/payments/"
                            + transaction.getGatewayUsed() + "/captures")
                    .addHeader("Authorization",
                            "Bearer " + checkoutConfig.getSecretKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "";

                log.info("Checkout.com capture response. code={}, body={}",
                        response.code(), responseBody);

                if (response.isSuccessful()) {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    String actionId = jsonNode.path("action_id").asText();
                    return GatewayResponse.success(actionId, responseBody);
                } else {
                    return GatewayResponse.failure(
                            "CAPTURE_ERROR", "Capture failed", responseBody);
                }
            }
        } catch (IOException e) {
            throw new GatewayException(
                    GATEWAY_NAME,
                    "GATEWAY_TIMEOUT",
                    "Checkout.com capture failed: " + e.getMessage(),
                    true,
                    e);
        }
    }

    /**
     * Refunds a Checkout.com payment.
     * POST /payments/{id}/refunds
     */
    @Override
    public GatewayResponse refund(Transaction transaction) {
        log.info("Refunding Checkout.com payment. txnId={}",
                transaction.getId());

        try {
            long amountInSmallestUnit = transaction.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", amountInSmallestUnit);
            requestBody.put("reference",
                    transaction.getId().toString());

            String json = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(checkoutConfig.getBaseUrl() + "/payments/"
                            + transaction.getGatewayUsed() + "/refunds")
                    .addHeader("Authorization",
                            "Bearer " + checkoutConfig.getSecretKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "";

                log.info("Checkout.com refund response. code={}, body={}",
                        response.code(), responseBody);

                if (response.isSuccessful()) {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    String actionId = jsonNode.path("action_id").asText();
                    return GatewayResponse.success(actionId, responseBody);
                } else {
                    return GatewayResponse.failure(
                            "REFUND_ERROR", "Refund failed", responseBody);
                }
            }
        } catch (IOException e) {
            throw new GatewayException(
                    GATEWAY_NAME,
                    "GATEWAY_TIMEOUT",
                    "Checkout.com refund failed: " + e.getMessage(),
                    true,
                    e);
        }
    }
}