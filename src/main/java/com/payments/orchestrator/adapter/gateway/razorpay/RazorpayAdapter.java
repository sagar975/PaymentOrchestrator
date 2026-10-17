package com.payments.orchestrator.adapter.gateway.razorpay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orchestrator.config.RazorpayConfig;
import com.payments.orchestrator.domain.exception.GatewayException;
import com.payments.orchestrator.domain.model.GatewayResponse;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.port.GatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Razorpay gateway adapter implementing GatewayPort.
 * <p>
 * Converts domain Transaction to Razorpay API calls
 * and normalizes Razorpay responses to GatewayResponse.
 * <p>
 * Uses OkHttp for HTTP calls — no Razorpay SDK dependency.
 * This makes it easy to test and swap out if needed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpayAdapter implements GatewayPort {

    private static final String GATEWAY_NAME = "razorpay";
    private static final String BASE_URL = "https://api.razorpay.com/v1";
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final RazorpayConfig razorpayConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String getGatewayName() {
        return GATEWAY_NAME;
    }

    /**
     * Creates a Razorpay order — equivalent to authorization.
     * Razorpay uses orders as the first step before payment capture.
     *
     * @param transaction the transaction to authorize
     * @return GatewayResponse with Razorpay order details
     * @throws GatewayException if Razorpay rejects or is unreachable
     */
    @Override
    public GatewayResponse authorize(Transaction transaction) {
        log.info("Authorizing transaction via Razorpay. txnId={}, amount={}",
                transaction.getId(), transaction.getAmount());

        try {
            // Razorpay amount is in paise (smallest currency unit)
            // ₹999.99 = 99999 paise
            long amountInPaise = transaction.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            String requestBody = objectMapper.writeValueAsString(
                    new RazorpayOrderRequest(
                            amountInPaise,
                            transaction.getCurrency(),
                            transaction.getIdempotencyKey()
                    )
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/orders")
                    .addHeader("Authorization", Credentials.basic(
                            razorpayConfig.getKeyId(),
                            razorpayConfig.getKeySecret()))
                    .post(RequestBody.create(requestBody, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "";

                log.debug("Razorpay response. status={}, body={}",
                        response.code(), responseBody);

                if (response.isSuccessful()) {
                    JsonNode json = objectMapper.readTree(responseBody);
                    String orderId = json.get("id").asText();

                    log.info("Razorpay order created. orderId={}, txnId={}",
                            orderId, transaction.getId());

                    return GatewayResponse.success(orderId, responseBody);
                } else {
                    JsonNode json = objectMapper.readTree(responseBody);
                    String errorCode = json.path("error")
                            .path("code").asText("GATEWAY_ERROR");
                    String errorDesc = json.path("error")
                            .path("description").asText("Unknown error");

                    log.warn("Razorpay rejected. code={}, desc={}, txnId={}",
                            errorCode, errorDesc, transaction.getId());

                    boolean retryable = isRetryable(response.code());
                    return GatewayResponse.failure(errorCode, errorDesc, responseBody);
                }
            }
        } catch (IOException e) {
            log.error("Razorpay network error. txnId={}", transaction.getId(), e);
            throw new GatewayException(
                    GATEWAY_NAME,
                    "GATEWAY_TIMEOUT",
                    "Razorpay is unreachable: " + e.getMessage(),
                    true,
                    e);
        } catch (Exception e) {
            log.error("Razorpay unexpected error. txnId={}",
                    transaction.getId(), e);
            throw new GatewayException(
                    GATEWAY_NAME,
                    "GATEWAY_ERROR",
                    "Unexpected error calling Razorpay",
                    false,
                    e);
        }
    }

    /**
     * Captures a Razorpay payment.
     * Called after customer completes payment on Razorpay checkout.
     */
    @Override
    public GatewayResponse capture(Transaction transaction) {
        log.info("Capturing payment via Razorpay. txnId={}",
                transaction.getId());

        try {
            long amountInPaise = transaction.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            String requestBody = objectMapper.writeValueAsString(
                    new RazorpayCaptureRequest(amountInPaise,
                            transaction.getCurrency())
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/payments/"
                            + transaction.getGatewayUsed() + "/capture")
                    .addHeader("Authorization", Credentials.basic(
                            razorpayConfig.getKeyId(),
                            razorpayConfig.getKeySecret()))
                    .post(RequestBody.create(requestBody, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "";

                if (response.isSuccessful()) {
                    JsonNode json = objectMapper.readTree(responseBody);
                    String paymentId = json.get("id").asText();
                    return GatewayResponse.success(paymentId, responseBody);
                } else {
                    JsonNode json = objectMapper.readTree(responseBody);
                    String errorCode = json.path("error")
                            .path("code").asText("CAPTURE_ERROR");
                    String errorDesc = json.path("error")
                            .path("description").asText("Capture failed");
                    return GatewayResponse.failure(
                            errorCode, errorDesc, responseBody);
                }
            }
        } catch (IOException e) {
            throw new GatewayException(
                    GATEWAY_NAME, "GATEWAY_TIMEOUT",
                    "Razorpay capture failed: " + e.getMessage(), true, e);
        }
    }

    /**
     * Refunds a Razorpay payment.
     */
    @Override
    public GatewayResponse refund(Transaction transaction) {
        log.info("Refunding payment via Razorpay. txnId={}",
                transaction.getId());

        try {
            long amountInPaise = transaction.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            String requestBody = objectMapper.writeValueAsString(
                    new RazorpayRefundRequest(amountInPaise)
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/payments/"
                            + transaction.getGatewayUsed() + "/refund")
                    .addHeader("Authorization", Credentials.basic(
                            razorpayConfig.getKeyId(),
                            razorpayConfig.getKeySecret()))
                    .post(RequestBody.create(requestBody, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "";

                if (response.isSuccessful()) {
                    JsonNode json = objectMapper.readTree(responseBody);
                    String refundId = json.get("id").asText();
                    return GatewayResponse.success(refundId, responseBody);
                } else {
                    JsonNode json = objectMapper.readTree(responseBody);
                    String errorCode = json.path("error")
                            .path("code").asText("REFUND_ERROR");
                    String errorDesc = json.path("error")
                            .path("description").asText("Refund failed");
                    return GatewayResponse.failure(
                            errorCode, errorDesc, responseBody);
                }
            }
        } catch (IOException e) {
            throw new GatewayException(
                    GATEWAY_NAME, "GATEWAY_TIMEOUT",
                    "Razorpay refund failed: " + e.getMessage(), true, e);
        }
    }

    /**
     * Determines if a failed request is worth retrying.
     * 5xx errors are retryable — server side issues.
     * 4xx errors are not — client side issues.
     */
    private boolean isRetryable(int httpStatusCode) {
        return httpStatusCode >= 500;
    }
}