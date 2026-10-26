package com.payments.orchestrator.adapter.web.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Recent transactions list for dashboard.
 */
@Getter
@Builder
public class RecentTransactionsResponse {

    private final List<TransactionSummary> transactions;

    @Getter
    @Builder
    public static class TransactionSummary {
        private final UUID id;
        private final String merchantId;
        private final BigDecimal amount;
        private final String currency;
        private final String status;
        private final String gatewayUsed;
        private final int retryCount;
        private final LocalDateTime createdAt;
    }
}