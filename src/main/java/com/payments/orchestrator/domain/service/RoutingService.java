package com.payments.orchestrator.domain.service;

import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.port.GatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Routing service — selects the best gateway for a transaction.
 * <p>
 * Routing rules applied in order:
 * 1. Currency support — gateway must support transaction currency
 * 2. Gateway priority — configured priority order
 * 3. Future: success rate based routing (Phase 4)
 * <p>
 * Adding a new gateway requires zero changes here —
 * just register it as a Spring bean implementing GatewayPort.
 */
@Slf4j
@Service
public class RoutingService {

    private final List<GatewayPort> availableGateways;

    /**
     * Spring auto-injects ALL beans implementing GatewayPort.
     * Adding a new gateway adapter automatically makes it
     * available for routing — no configuration needed.
     *
     * @param availableGateways all registered gateway adapters
     */
    public RoutingService(List<GatewayPort> availableGateways) {
        this.availableGateways = availableGateways;
        log.info("RoutingService initialized with {} gateways: {}",
                availableGateways.size(),
                availableGateways.stream()
                        .map(GatewayPort::getGatewayName)
                        .toList());
    }

    /**
     * Selects the most appropriate gateway for the transaction.
     * <p>
     * Currently uses priority-based routing.
     * Phase 4 will add success-rate based routing.
     *
     * @param transaction the transaction to route
     * @return the selected GatewayPort
     * @throws IllegalStateException if no gateway available
     */
    public GatewayPort selectGateway(Transaction transaction) {
        log.info("Selecting gateway. txnId={}, currency={}, amount={}",
                transaction.getId(),
                transaction.getCurrency(),
                transaction.getAmount());

        GatewayPort selected = availableGateways.stream()
                .filter(gateway -> supportsTransaction(gateway, transaction))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No gateway available for transaction: "
                                + transaction.getId()));

        log.info("Gateway selected. txnId={}, gateway={}",
                transaction.getId(),
                selected.getGatewayName());

        return selected;
    }

    /**
     * Selects a fallback gateway — different from the one that failed.
     * Used by retry logic when primary gateway fails.
     *
     * @param transaction       the failed transaction
     * @param failedGatewayName name of the gateway that failed
     * @return alternative gateway if available
     */
    public GatewayPort selectFallbackGateway(
            Transaction transaction,
            String failedGatewayName) {

        log.info("Selecting fallback gateway. txnId={}, failed={}",
                transaction.getId(), failedGatewayName);

        return availableGateways.stream()
                .filter(gateway ->
                        !gateway.getGatewayName().equals(failedGatewayName))
                .filter(gateway -> supportsTransaction(gateway, transaction))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No fallback gateway available for transaction: "
                                + transaction.getId()));
    }

    /**
     * Returns all available gateways — used for health checks
     * and admin dashboard.
     */
    public List<String> getAvailableGatewayNames() {
        return availableGateways.stream()
                .map(GatewayPort::getGatewayName)
                .toList();
    }

    /**
     * Checks if a gateway supports the given transaction.
     * Currently checks currency support.
     * Phase 4 will add amount limits, merchant config etc.
     */
    private boolean supportsTransaction(
            GatewayPort gateway,
            Transaction transaction) {
        boolean supported = getSupportedCurrencies(gateway)
                .contains(transaction.getCurrency());

        if (!supported) {
            log.debug("Gateway {} does not support currency {}",
                    gateway.getGatewayName(),
                    transaction.getCurrency());
        }

        return supported;
    }

    /**
     * Returns currencies supported by each gateway.
     * India-focused: INR supported by all Indian gateways.
     * Will expand as we add global gateways.
     */
    private List<String> getSupportedCurrencies(GatewayPort gateway) {
        return switch (gateway.getGatewayName()) {
            case "razorpay" -> List.of("INR");           // India only
            case "payu"     -> List.of("INR");           // India only
            case "cashfree" -> List.of("INR");           // India only
            case "stripe"   -> List.of("USD", "GBP", "EUR", "SGD", "AED", "INR");
            default         -> List.of("INR");
        };
    }
}