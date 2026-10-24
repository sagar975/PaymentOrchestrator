package com.payments.orchestrator.adapter.web.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orchestrator.domain.model.TransactionStatus;
import com.payments.orchestrator.domain.port.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handles Stripe webhook events.
 * Verifies signature and updates transaction status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StripeWebhookHandler {

    @Value("${stripe.webhook.secret:webhook_secret}")
    private String webhookSecret;

    private final WebhookVerificationService verificationService;
    private final TransactionRepositoryPort transactionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Processes incoming Stripe webhook.
     *
     * @param payload   raw webhook body
     * @param signature Stripe-Signature header
     */
    public void handle(String payload, String signature) {
        // Step 1 — verify signature
        if (!verificationService.verifyStripe(payload, signature, webhookSecret)) {
            log.warn("Invalid Stripe webhook signature");
            return;
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("type").asText();

            log.info("Processing Stripe webhook. event={}", eventType);

            switch (eventType) {
                case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
                case "payment_intent.payment_failed" -> handlePaymentFailed(event);
                case "payment_intent.canceled" -> handlePaymentCanceled(event);
                default -> log.debug("Unhandled Stripe event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
        }
    }

    private void handlePaymentSucceeded(JsonNode event) {
        String paymentIntentId = event.path("data").path("object").path("id").asText();
        String transactionId = event.path("data").path("object").path("metadata").path("transactionId").asText();

        log.info("Stripe payment succeeded. paymentIntentId={}", paymentIntentId);

        updateTransactionStatus(transactionId, TransactionStatus.CAPTURED, "Stripe webhook: payment succeeded");
    }

    private void handlePaymentFailed(JsonNode event) {
        String paymentIntentId = event.path("data").path("object").path("id").asText();

        log.info("Stripe payment failed. paymentIntentId={}", paymentIntentId);
    }

    private void handlePaymentCanceled(JsonNode event) {
        String paymentIntentId = event.path("data").path("object").path("id").asText();

        log.info("Stripe payment canceled. paymentIntentId={}", paymentIntentId);
    }

    private void updateTransactionStatus(String transactionId, TransactionStatus newStatus, String description) {
        try {
            if (transactionId == null || transactionId.isBlank()) {
                return;
            }
            transactionRepository.findById(java.util.UUID.fromString(transactionId)).ifPresent(transaction -> {
                transaction.transitionTo(newStatus);
                transactionRepository.save(transaction);
                log.info("Transaction updated via Stripe webhook. " + "id={}, status={}", transactionId, newStatus);
            });
        } catch (Exception e) {
            log.error("Error updating transaction from Stripe webhook", e);
        }
    }
}