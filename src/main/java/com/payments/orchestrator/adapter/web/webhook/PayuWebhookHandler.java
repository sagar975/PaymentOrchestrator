package com.payments.orchestrator.adapter.web.webhook;

import com.payments.orchestrator.config.PayuConfig;
import com.payments.orchestrator.domain.model.TransactionEvent;
import com.payments.orchestrator.domain.model.TransactionStatus;
import com.payments.orchestrator.domain.port.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.payments.orchestrator.adapter.gateway.payu.PayuHashUtil;

import java.util.Map;
import java.util.UUID;

/**
 * Handles PayU webhook events.
 * PayU sends form-encoded POST with transaction details.
 * Verified using SHA-512 hash.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayuWebhookHandler {

    private final TransactionRepositoryPort transactionRepository;
    private final PayuConfig payuConfig;

    /**
     * Processes incoming PayU webhook.
     * PayU sends form data — not JSON.
     *
     * @param params form parameters from PayU webhook
     */
    public void handle(Map<String, String> params) {
        try {
            String status = params.get("status");
            String txnId = params.get("udf1"); // we store transactionId in udf1
            String payuTransactionId = params.get("mihpayid");

            log.info("Processing PayU webhook. status={}, txnId={}", status, txnId);

            // Verify hash
            if (!verifyHash(params)) {
                log.warn("Invalid PayU webhook hash. txnId={}", txnId);
                return;
            }

            switch (status) {
                case "success" ->
                        updateTransactionStatus(txnId, TransactionStatus.AUTHORIZED, "PayU webhook: payment success. payuId=" + payuTransactionId);
                case "failure" ->
                        updateTransactionStatus(txnId, TransactionStatus.FAILED, "PayU webhook: payment failed");
                case "pending" -> log.info("PayU payment pending. txnId={}", txnId);
                default -> log.warn("Unknown PayU status: {}", status);
            }
        } catch (Exception e) {
            log.error("Error processing PayU webhook", e);
        }
    }

    /**
     * Verifies PayU webhook hash.
     * PayU hash format (reverse of payment hash):
     * salt|status|udf5|udf4|udf3|udf2|udf1|email|
     * firstname|productinfo|amount|txnid|key
     */
    private boolean verifyHash(Map<String, String> params) {
        try {
            String hashSequence = payuConfig.getMerchantSalt() + "|" + params.getOrDefault("status", "") + "|" + params.getOrDefault("udf5", "") + "|" + params.getOrDefault("udf4", "") + "|" + params.getOrDefault("udf3", "") + "|" + params.getOrDefault("udf2", "") + "|" + params.getOrDefault("udf1", "") + "|" + params.getOrDefault("email", "") + "|" + params.getOrDefault("firstname", "") + "|" + params.getOrDefault("productinfo", "") + "|" + params.getOrDefault("amount", "") + "|" + params.getOrDefault("txnid", "") + "|" + payuConfig.getMerchantKey();

            String computedHash = PayuHashUtil.generateHash(payuConfig.getMerchantKey(), params.getOrDefault("txnid", ""), params.getOrDefault("amount", ""), params.getOrDefault("productinfo", ""), params.getOrDefault("firstname", ""), params.getOrDefault("email", ""), payuConfig.getMerchantSalt());

            String receivedHash = params.getOrDefault("hash", "");
            return computedHash.equals(receivedHash);

        } catch (Exception e) {
            log.error("Error verifying PayU hash", e);
            return false;
        }
    }

    private void updateTransactionStatus(String transactionId, TransactionStatus newStatus, String description) {
        try {
            if (transactionId == null || transactionId.isBlank()) {
                log.warn("No transactionId in PayU webhook");
                return;
            }

            transactionRepository.findById(UUID.fromString(transactionId)).ifPresent(transaction -> {
                transaction.transitionTo(newStatus);
                transactionRepository.save(transaction);

                transactionRepository.saveEvent(new TransactionEvent(transaction.getId(), transaction.getStatus(), newStatus, null, description));

                log.info("Transaction updated via PayU webhook. " + "id={}, status={}", transactionId, newStatus);
            });
        } catch (Exception e) {
            log.error("Error updating transaction from PayU webhook. " + "id={}", transactionId, e);
        }
    }
}