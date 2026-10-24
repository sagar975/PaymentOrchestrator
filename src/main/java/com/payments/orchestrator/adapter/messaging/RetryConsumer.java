package com.payments.orchestrator.adapter.messaging;

import com.payments.orchestrator.domain.exception.GatewayException;
import com.payments.orchestrator.domain.model.GatewayResponse;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.model.TransactionEvent;
import com.payments.orchestrator.domain.model.TransactionStatus;
import com.payments.orchestrator.domain.port.GatewayPort;
import com.payments.orchestrator.domain.port.TransactionRepositoryPort;
import com.payments.orchestrator.domain.service.RoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for transaction retry/failover.
 * <p>
 * Listens for TransactionFailedEvent and retries
 * the transaction on a different gateway automatically.
 * <p>
 * This is the core of the orchestration platform —
 * automatic failover without merchant intervention.
 * Merchant never knows a retry happened.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryConsumer {

    private static final String TOPIC_TRANSACTION_FAILED = "payment.transaction.failed";

    private final TransactionRepositoryPort transactionRepository;
    private final RoutingService routingService;

    /**
     * Processes failed transaction events and retries
     * on a fallback gateway.
     *
     * @param event the failed transaction event from Kafka
     */
    @KafkaListener(topics = TOPIC_TRANSACTION_FAILED, groupId = "payment-orchestrator-retry")
    public void handleTransactionFailed(TransactionFailedEvent event) {

        log.info("Received retry event. txnId={}, failedGateway={}", event.getTransactionId(), event.getFailedGateway());

        try {
            // Load transaction from DB
            Transaction transaction = transactionRepository.findById(event.getTransactionId()).orElseThrow(() -> new IllegalStateException("Transaction not found: " + event.getTransactionId()));

            // Check if retry is still allowed
            if (!transaction.canRetry()) {
                log.warn("Max retries reached. txnId={}, retryCount={}", transaction.getId(), transaction.getRetryCount());

                transaction.transitionTo(TransactionStatus.FAILED_FINAL);
                transactionRepository.save(transaction);

                transactionRepository.saveEvent(new TransactionEvent(transaction.getId(), TransactionStatus.FAILED, TransactionStatus.FAILED_FINAL, null, "Max retry attempts reached"));
                return;
            }

            // Select fallback gateway — different from failed one
            GatewayPort fallbackGateway;
            try {
                fallbackGateway = routingService.selectFallbackGateway(transaction, event.getFailedGateway());
            } catch (IllegalStateException noFallback) {
                log.warn("No fallback gateway available. txnId={}, currency={}", transaction.getId(), transaction.getCurrency());

                transaction.transitionTo(TransactionStatus.FAILED_FINAL);
                transactionRepository.save(transaction);

                transactionRepository.saveEvent(new TransactionEvent(transaction.getId(), TransactionStatus.FAILED, TransactionStatus.FAILED_FINAL, null, "No fallback gateway available for currency: " + transaction.getCurrency()));
                return;
            }

            // Transition back to ROUTING
            transaction.transitionTo(TransactionStatus.ROUTING);
            transaction.assignGateway(fallbackGateway.getGatewayName());
            transactionRepository.save(transaction);

            transactionRepository.saveEvent(new TransactionEvent(transaction.getId(), TransactionStatus.RETRYING, TransactionStatus.ROUTING, null, "Routing to fallback: " + fallbackGateway.getGatewayName()));

            // Attempt authorization on fallback gateway
            GatewayResponse response = fallbackGateway.authorize(transaction);

            if (response.isSuccess()) {
                transaction.transitionTo(TransactionStatus.AUTHORIZED);
                transactionRepository.save(transaction);

                transactionRepository.saveEvent(new TransactionEvent(transaction.getId(), TransactionStatus.ROUTING, TransactionStatus.AUTHORIZED, response.getRawResponse(), "Authorized by fallback: " + fallbackGateway.getGatewayName()));

                log.info("Retry successful. txnId={}, gateway={}", transaction.getId(), fallbackGateway.getGatewayName());

            } else {
                transaction.markFailed();
                transactionRepository.save(transaction);

                transactionRepository.saveEvent(new TransactionEvent(transaction.getId(), TransactionStatus.ROUTING, TransactionStatus.FAILED, response.getRawResponse(), "Fallback also failed: " + response.getErrorMessage()));

                log.warn("Fallback gateway also failed. txnId={}, error={}", transaction.getId(), response.getErrorMessage());
            }

        } catch (GatewayException e) {
            log.error("Gateway exception during retry. txnId={}", event.getTransactionId(), e);
        } catch (Exception e) {
            log.error("Unexpected error during retry. txnId={}", event.getTransactionId(), e);
        }
    }
}