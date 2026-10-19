package com.payments.orchestrator.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit record of a transaction state change.
 * <p>
 * Every status transition on a Transaction produces one TransactionEvent.
 * These events are never deleted — they form a permanent audit trail.
 * <p>
 * Immutability is intentional — audit records must never be modified
 * after creation.
 */
public class TransactionEvent {

    private final UUID id;
    private final UUID transactionId;

    private final TransactionStatus fromStatus;
    private final TransactionStatus toStatus;

    private final String gatewayResponse;

    private final String description;

    private final LocalDateTime occurredAt;

    /**
     * Creates a new immutable transaction event.
     *
     * @param transactionId   the transaction this event belongs to
     * @param fromStatus      status before transition —
     *                        null allowed for initial INITIATED event
     * @param toStatus        status after the transition
     * @param gatewayResponse raw gateway response — null if not applicable
     * @param description     human readable description of what happened
     */
    public TransactionEvent(UUID transactionId, TransactionStatus fromStatus, TransactionStatus toStatus, String gatewayResponse, String description) {

        if (transactionId == null) {
            throw new IllegalArgumentException("transactionId can not be null");
        }

        if (toStatus == null) {
            throw new IllegalArgumentException("toStatus can not be null");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }
        this.id = UUID.randomUUID();
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.gatewayResponse = gatewayResponse;
        this.description = description;
        this.transactionId = transactionId;
        this.occurredAt   = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;

    }

    public TransactionStatus getFromStatus() {
        return fromStatus;
    }

    public TransactionStatus getToStatus() {
        return toStatus;

    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public String getDescription() {
        return description;
    }

}
