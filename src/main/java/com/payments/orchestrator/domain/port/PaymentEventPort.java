package com.payments.orchestrator.domain.port;

import com.payments.orchestrator.domain.model.Transaction;

/**
 * Outbound port for publishing payment events.
 * <p>
 * Implemented by PaymentEventProducer in adapter/messaging layer.
 * Domain layer depends only on this interface —
 * never on Kafka directly.
 */
public interface PaymentEventPort {

    /**
     * Publishes a transaction failed event for async retry.
     * Consumer will pick this up and retry on fallback gateway.
     *
     * @param transaction       the failed transaction
     * @param failedGatewayName name of the gateway that failed
     */
    void publishTransactionFailed(Transaction transaction, String failedGatewayName);
}