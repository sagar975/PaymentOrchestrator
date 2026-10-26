package com.payments.orchestrator.adapter.web.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Overall platform summary metrics.
 */
@Getter
@Builder
public class SummaryResponse {

    private final long totalTransactions;
    private final long successfulTransactions;
    private final long failedTransactions;
    private final double successRate;
    private final BigDecimal totalVolume;
    private final long retriedTransactions;
    private final long kafkaRetriesSuccessful;
}