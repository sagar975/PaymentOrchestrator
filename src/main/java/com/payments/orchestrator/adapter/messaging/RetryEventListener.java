package com.payments.orchestrator.adapter.messaging;

import com.payments.orchestrator.domain.service.TransactionRetryRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for retry events AFTER database transaction commits.
 * Only then publishes to Kafka — guarantees transaction exists in DB
 * when RetryConsumer queries for it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryEventListener {

    private final PaymentEventProducer eventProducer;

    /**
     * Called AFTER the database transaction commits successfully.
     * Safe to publish to Kafka now — transaction is in DB.
     *
     * @param event the retry requested event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRetryRequested(TransactionRetryRequestedEvent event) {
        log.info("DB committed — publishing Kafka retry event. txnId={}",
                event.getTransaction().getId());

        eventProducer.publishTransactionFailed(
                event.getTransaction(),
                event.getFailedGatewayName());
    }
}