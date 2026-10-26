package com.payments.orchestrator.adapter.web.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-gateway performance metrics.
 */
@Getter
@Builder
public class GatewayPerformanceResponse {

    private final List<GatewayStats> gateways;

    @Getter
    @Builder
    public static class GatewayStats {
        private final String gatewayName;
        private final long totalTransactions;
        private final long successfulTransactions;
        private final long failedTransactions;
        private final double successRate;
        private final BigDecimal totalVolume;
    }
}