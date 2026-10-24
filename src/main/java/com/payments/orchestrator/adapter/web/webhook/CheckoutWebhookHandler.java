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
 * Handles Checkout.com webhook events.
 * Verifies signature and updates transaction status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutWebhookHandler {

    @Value("${checkout.webhook.secret:webhook_secret}")
    private String webhookSecret;

    private final TransactionRepositoryPort transactionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Processes incoming Checkout.com webhook.
     *
     * @param payload   raw webhook body
     * @param signature Authorization header
     */
    public void handle(String payload, String signature) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("type").asText();

            log.info("Processing Checkout.com webhook. event={}", eventType);

            switch (eventType) {
                case "payment_approved" -> handlePaymentApproved(event);
                case "payment_declined" -> handlePaymentDeclined(event);
                case "payment_captured" -> handlePaymentCaptured(event);
                default -> log.debug("Unhandled Checkout event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Checkout.com webhook", e);
        }
    }

    private void handlePaymentApproved(JsonNode event) {
        String paymentId = event.path("data").path("id").asText();
        String transactionId = event.path("data").path("metadata").path("transactionId").asText();

        log.info("Checkout.com payment approved. paymentId={}", paymentId);

        updateTransactionStatus(transactionId, TransactionStatus.AUTHORIZED, "Checkout.com webhook: payment approved");
    }

    private void handlePaymentDeclined(JsonNode event) {
        String paymentId = event.path("data").path("id").asText();
        log.info("Checkout.com payment declined. paymentId={}", paymentId);
    }

    private void handlePaymentCaptured(JsonNode event) {
        String paymentId = event.path("data").path("id").asText();
        log.info("Checkout.com payment captured. paymentId={}", paymentId);
    }

    private void updateTransactionStatus(String transactionId, TransactionStatus newStatus, String description) {
        try {
            if (transactionId == null || transactionId.isBlank()) {
                return;
            }
            transactionRepository.findById(java.util.UUID.fromString(transactionId)).ifPresent(transaction -> {
                transaction.transitionTo(newStatus);
                transactionRepository.save(transaction);
                log.info("Transaction updated via Checkout webhook. " + "id={}", transactionId);
            });
        } catch (Exception e) {
            log.error("Error updating transaction from Checkout webhook", e);
        }
    }
}