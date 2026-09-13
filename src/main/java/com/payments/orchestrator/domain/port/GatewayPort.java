package com.payments.orchestrator.domain.port;

import com.payments.orchestrator.domain.model.GatewayResponse;
import com.payments.orchestrator.domain.model.Transaction;

/**
 * Outbound port defining the contract every payment gateway must fulfill.
 * <p>
 * Each gateway adapter (Razorpay, Stripe, PayU) implements this interface.
 * The domain layer depends only on this interface — never on concrete
 * gateway implementations. This allows gateways to be added, removed,
 * or swapped without touching domain logic.
 * <p>
 * All implementations must be idempotent — calling the same method
 * twice with the same transaction must not double-charge the customer.
 */
public interface GatewayPort {
    /**
     * Authorizes a payment — reserves funds on customer's account.
     * Does not debit the customer yet.
     *
     * @param transaction the transaction to authorize
     * @return GatewayResponse containing authorization result
     * @throws com.payments.orchestrator.domain.exception.GatewayException if gateway rejects or is unreachable
     */
    GatewayResponse authorize(Transaction transaction);

    /**
     * Captures a previously authorized payment.
     * Debits the customer and transfers funds to merchant.
     *
     * @param transaction the authorized transaction to capture
     * @return GatewayResponse containing capture result
     * @throws com.payments.orchestrator.domain.exception.GatewayException if capture fails
     */
    GatewayResponse capture(Transaction transaction);

    /**
     * Refunds a captured payment — returns funds to customer.
     *
     * @param transaction the captured transaction to refund
     * @return GatewayResponse containing refund result
     * @throws com.payments.orchestrator.domain.exception.GatewayException if refund fails
     */
    GatewayResponse refund(Transaction transaction);

    /**
     * Returns the unique name of this gateway.
     * Used by routing engine to identify and select gateways.
     * Must be lowercase e.g. "razorpay", "stripe", "payu"
     *
     * @return gateway name
     */
    String getGatewayName();

}
