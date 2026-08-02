package com.payments.orchestrator.domain.model;

/**
 * Represents the lifecycle states of a payment transaction.
 * <p>
 * State transition rules:
 * INITIATED   → ROUTING
 * ROUTING     → AUTHORIZED | FAILED
 * AUTHORIZED  → CAPTURED   | FAILED
 * CAPTURED    → SETTLED    | FAILED
 * FAILED      → RETRYING   | FAILED_FINAL
 * RETRYING    → ROUTING    | FAILED_FINAL
 * <p>
 * Terminal states: SETTLED, FAILED_FINAL
 * Once in a terminal state, no further transitions are allowed.
 */
public enum TransactionStatus {

    /**
     * Transaction has been received but not yet routed to a gateway.
     */
    INITIATED,

    /**
     * Routing engine is selecting the appropriate payment gateway.
     */
    ROUTING,

    /**
     * Payment has been authorized by the gateway — funds reserved.
     * Not yet captured (debited) from customer.
     */
    AUTHORIZED,
    /**
     * Payment has been captured — funds debited from customer.
     */
    CAPTURED,
    /**
     * Payment fully settled — funds transferred to merchant.
     * TERMINAL STATE — no further transitions allowed.
     */
    SETTLED,
    /**
     * Transaction failed at gateway — retry may be attempted.
     */
    FAILED,
    /**
     * Retry in progress — routing to alternate gateway.
     */
    RETRYING,

    /**
     * All retry attempts exhausted — transaction permanently failed.
     * TERMINAL STATE — no further transitions allowed.
     */
    FAILED_FINAL;

    /**
     * Returns true if this state is terminal.
     * Terminal states cannot transition to any other state.
     */
    public boolean isTerminal() {
        return this == SETTLED || this == FAILED_FINAL;
    }

    /**
     * Returns true if this state allows retry attempts.
     */
    public boolean isRetryable() {
        return this == FAILED;
    }
}
