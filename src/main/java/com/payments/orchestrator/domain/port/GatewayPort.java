package com.payments.orchestrator.domain.port;

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

}
