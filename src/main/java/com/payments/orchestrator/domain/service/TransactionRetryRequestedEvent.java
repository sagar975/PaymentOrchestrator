package com.payments.orchestrator.domain.service;

import com.payments.orchestrator.domain.model.Transaction;

/**
 * Spring application event fired after transaction is saved.
 * Kafka message published only after DB commit completes.
 */
public class TransactionRetryRequestedEvent {

    private final Transaction transaction;
    private final String failedGatewayName;

    public TransactionRetryRequestedEvent(Transaction transaction, String failedGatewayName) {
        this.transaction = transaction;
        this.failedGatewayName = failedGatewayName;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public String getFailedGatewayName() {
        return failedGatewayName;
    }
}