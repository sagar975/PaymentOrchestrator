package com.payments.orchestrator.adapter.web.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.model.TransactionEvent;
import com.payments.orchestrator.domain.model.TransactionStatus;
import com.payments.orchestrator.domain.port.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Handles Razorpay webhook events.
 * Verifies signature and updates transaction status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpayWebhookHandler {

    @Value("${razorpay.webhook.secret:webhook_secret}")
    private String webhookSecret;

    private final WebhookVerificationService verificationService;
    private final TransactionRepositoryPort transactionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Processes incoming Razorpay webhook.
     *
     * @param payload   raw webhook body
     * @param signature X-Razorpay-Signature header
     */
    public void handle(String payload, String signature) {
        // Step 1 — verify signature
        if (!verificationService.verifyRazorpay(payload, signature, webhookSecret)) {
            log.warn("Invalid Razorpay webhook signature");
            return;
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("event").asText();

            log.info("Processing Razorpay webhook. event={}", eventType);

            switch (eventType) {
                case "payment.authorized" -> handlePaymentAuthorized(event);
                case "payment.captured" -> handlePaymentCaptured(event);
                case "payment.failed" -> handlePaymentFailed(event);
                default -> log.debug("Unhandled Razorpay event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
        }
    }

    private void handlePaymentAuthorized(JsonNode event) {
        String paymentId = event.path("payload").path("payment").path("entity").path("id").asText();
        String notes = event.path("payload").path("payment").path("entity").path("notes").path("transactionId").asText();

        log.info("Razorpay payment authorized. paymentId={}", paymentId);

        updateTransactionStatus(notes, TransactionStatus.AUTHORIZED, "Razorpay webhook: payment authorized");
    }

    private void handlePaymentCaptured(JsonNode event) {
        String paymentId = event.path("payload").path("payment").path("entity").path("id").asText();

        log.info("Razorpay payment captured. paymentId={}", paymentId);
    }

    private void handlePaymentFailed(JsonNode event) {
        String paymentId = event.path("payload").path("payment").path("entity").path("id").asText();

        log.info("Razorpay payment failed. paymentId={}", paymentId);
    }

    private void updateTransactionStatus(String transactionId, TransactionStatus newStatus, String description) {
        try {
            if (transactionId == null || transactionId.isBlank()) {
                log.warn("No transactionId in webhook payload");
                return;
            }

            transactionRepository.findById(UUID.fromString(transactionId)).ifPresent(transaction -> {
                transaction.transitionTo(newStatus);
                transactionRepository.save(transaction);
                transactionRepository.saveEvent(new TransactionEvent(transaction.getId(), transaction.getStatus(), newStatus, null, description));
                log.info("Transaction updated via webhook. " + "id={}, status={}", transactionId, newStatus);
            });
        } catch (Exception e) {
            log.error("Error updating transaction. id={}", transactionId, e);
        }
    }
}