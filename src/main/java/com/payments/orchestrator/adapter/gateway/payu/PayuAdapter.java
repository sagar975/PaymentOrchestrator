package com.payments.orchestrator.adapter.gateway.payu;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orchestrator.config.PayuConfig;
import com.payments.orchestrator.domain.exception.GatewayException;
import com.payments.orchestrator.domain.model.GatewayResponse;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.port.GatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PayU gateway adapter implementing GatewayPort.
 * <p>
 * PayU India uses form-based POST requests with SHA-512 hash
 * authentication — different from Razorpay's JSON + basic auth.
 * This adapter normalizes PayU responses to GatewayResponse.
 * <p>
 * Acts as fallback gateway when Razorpay fails.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PayuAdapter implements GatewayPort {

    private static final String GATEWAY_NAME = "payu";
    private static final String DEFAULT_PRODUCT_INFO = "Payment";
    private static final String DEFAULT_FIRST_NAME = "Customer";
    private static final String DEFAULT_EMAIL = "customer@example.com";

    private final OkHttpClient httpClient;
    private final PayuConfig payuConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String getGatewayName() {
        return GATEWAY_NAME;
    }

    /**
     * Initiates a PayU payment request.
     * <p>
     * PayU flow is different from Razorpay:
     * - Razorpay: server-side order creation → client completes payment
     * - PayU: server validates hash → client redirected to PayU page
     * <p>
     * For orchestration purposes we validate the hash generation
     * and confirm PayU is reachable.
     *
     * @param transaction the transaction to authorize
     * @return GatewayResponse with PayU transaction details
     */
    @Override
    public GatewayResponse authorize(Transaction transaction) {
        log.info("Authorizing transaction via PayU. txnId={}, amount={}", transaction.getId(), transaction.getAmount());

        try {
            String txnId = transaction.getId().toString();
            String amount = transaction.getAmount().toString();

            String hash = PayuHashUtil.generateHash(payuConfig.getMerchantKey(), txnId, amount, DEFAULT_PRODUCT_INFO, DEFAULT_FIRST_NAME, DEFAULT_EMAIL, payuConfig.getMerchantSalt());

            // Build form body for PayU request
            FormBody formBody = new FormBody.Builder().add("key", payuConfig.getMerchantKey()).add("txnid", txnId).add("amount", amount).add("productinfo", DEFAULT_PRODUCT_INFO).add("firstname", DEFAULT_FIRST_NAME).add("email", DEFAULT_EMAIL).add("currency", transaction.getCurrency()).add("hash", hash).build();

            Request request = new Request.Builder().url(payuConfig.getBaseUrl() + "/_payment").post(formBody).build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                log.debug("PayU response. status={}", response.code());

                // PayU returns redirect for valid requests
                // 200 or 302 means request was accepted
                if (response.isSuccessful() || response.code() == 302) {
                    log.info("PayU payment initiated. txnId={}", transaction.getId());
                    return GatewayResponse.success(txnId, responseBody);
                } else {
                    log.warn("PayU rejected. status={}, txnId={}", response.code(), transaction.getId());
                    return GatewayResponse.failure("PAYU_ERROR", "PayU rejected the request", responseBody);
                }
            }

        } catch (IOException e) {
            log.error("PayU network error. txnId={}", transaction.getId(), e);
            throw new GatewayException(GATEWAY_NAME, "GATEWAY_TIMEOUT", "PayU is unreachable: " + e.getMessage(), true, e);
        } catch (Exception e) {
            log.error("PayU unexpected error. txnId={}", transaction.getId(), e);
            throw new GatewayException(GATEWAY_NAME, "GATEWAY_ERROR", "Unexpected error calling PayU", false, e);
        }
    }

    /**
     * Captures a PayU payment.
     * PayU capture happens automatically after authorization.
     */
    @Override
    public GatewayResponse capture(Transaction transaction) {
        log.info("Capturing PayU payment. txnId={}", transaction.getId());
        // PayU auto-captures — return success
        return GatewayResponse.success(transaction.getId().toString(), "{\"status\":\"captured\"}");
    }

    /**
     * Refunds a PayU payment via PayU Refund API.
     */
    @Override
    public GatewayResponse refund(Transaction transaction) {
        log.info("Refunding PayU payment. txnId={}", transaction.getId());

        try {
            String hash = PayuHashUtil.generateHash(payuConfig.getMerchantKey(), transaction.getId().toString(), transaction.getAmount().toString(), DEFAULT_PRODUCT_INFO, DEFAULT_FIRST_NAME, DEFAULT_EMAIL, payuConfig.getMerchantSalt());

            FormBody formBody = new FormBody.Builder().add("key", payuConfig.getMerchantKey()).add("command", "cancel_refund_transaction").add("var1", transaction.getGatewayUsed()).add("hash", hash).build();

            Request request = new Request.Builder().url(payuConfig.getBaseUrl() + "/merchant/postservice.php?form=2").post(formBody).build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    return GatewayResponse.success(transaction.getId().toString(), responseBody);
                } else {
                    return GatewayResponse.failure("REFUND_ERROR", "Refund failed", responseBody);
                }
            }
        } catch (IOException e) {
            throw new GatewayException(GATEWAY_NAME, "GATEWAY_TIMEOUT", "PayU refund failed: " + e.getMessage(), true, e);
        }
    }
}