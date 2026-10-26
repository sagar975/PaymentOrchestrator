package com.payments.orchestrator.adapter.web.dashboard;

import com.payments.orchestrator.adapter.persistence.TransactionJpaEntity;
import com.payments.orchestrator.domain.model.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for dashboard analytics.
 * Aggregates transaction data into meaningful metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    /**
     * Returns overall platform summary stats.
     */
    @Transactional(readOnly = true)
    public SummaryResponse getSummary() {
        List<Object[]> statusCounts =
                dashboardRepository.countByStatus();

        long total = 0;
        long successful = 0;
        long failed = 0;
        long retried = 0;

        for (Object[] row : statusCounts) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            total += count;

            if (status.equals("AUTHORIZED")
                    || status.equals("CAPTURED")
                    || status.equals("SETTLED")) {
                successful += count;
            }
            if (status.equals("FAILED")
                    || status.equals("FAILED_FINAL")) {
                failed += count;
            }
            if (status.equals("RETRYING")) {
                retried += count;
            }
        }

        double successRate = total > 0
                ? Math.round((double) successful / total * 10000.0) / 100.0
                : 0.0;

        // Calculate total volume from all transactions
        BigDecimal totalVolume = dashboardRepository.findAll()
                .stream()
                .map(TransactionJpaEntity::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SummaryResponse.builder()
                .totalTransactions(total)
                .successfulTransactions(successful)
                .failedTransactions(failed)
                .successRate(successRate)
                .totalVolume(totalVolume)
                .retriedTransactions(retried)
                .kafkaRetriesSuccessful(0L)
                .build();
    }

    /**
     * Returns per-gateway performance metrics.
     */
    @Transactional(readOnly = true)
    public GatewayPerformanceResponse getGatewayPerformance() {
        List<Object[]> stats = dashboardRepository.getGatewayStats();

        List<GatewayPerformanceResponse.GatewayStats> gatewayStats =
                stats.stream().map(row -> {
                    String gateway = row[0].toString();
                    long total = ((Number) row[1]).longValue();
                    BigDecimal volume = row[2] != null
                            ? new BigDecimal(row[2].toString())
                            : BigDecimal.ZERO;
                    long successful = ((Number) row[3]).longValue();
                    long failedCount = total - successful;

                    double rate = total > 0
                            ? Math.round((double) successful / total * 10000.0) / 100.0
                            : 0.0;

                    return GatewayPerformanceResponse.GatewayStats.builder()
                            .gatewayName(gateway)
                            .totalTransactions(total)
                            .successfulTransactions(successful)
                            .failedTransactions(failedCount)
                            .successRate(rate)
                            .totalVolume(volume)
                            .build();
                }).collect(Collectors.toList());

        return GatewayPerformanceResponse.builder()
                .gateways(gatewayStats)
                .build();
    }

    /**
     * Returns transaction count broken down by status.
     */
    @Transactional(readOnly = true)
    public StatusBreakdownResponse getStatusBreakdown() {
        List<Object[]> statusCounts =
                dashboardRepository.countByStatus();

        Map<String, Long> counts = new HashMap<>();
        long total = 0;

        for (Object[] row : statusCounts) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            counts.put(status, count);
            total += count;
        }

        return StatusBreakdownResponse.builder()
                .initiated(counts.getOrDefault("INITIATED", 0L))
                .routing(counts.getOrDefault("ROUTING", 0L))
                .authorized(counts.getOrDefault("AUTHORIZED", 0L))
                .captured(counts.getOrDefault("CAPTURED", 0L))
                .settled(counts.getOrDefault("SETTLED", 0L))
                .failed(counts.getOrDefault("FAILED", 0L))
                .retrying(counts.getOrDefault("RETRYING", 0L))
                .failedFinal(counts.getOrDefault("FAILED_FINAL", 0L))
                .total(total)
                .build();
    }

    /**
     * Returns most recent transactions.
     */
    @Transactional(readOnly = true)
    public RecentTransactionsResponse getRecentTransactions(int limit) {
        List<TransactionJpaEntity> recent =
                dashboardRepository.findRecentTransactions(
                        PageRequest.of(0, Math.min(limit, 50)));

        List<RecentTransactionsResponse.TransactionSummary> summaries =
                recent.stream().map(t ->
                        RecentTransactionsResponse.TransactionSummary
                                .builder()
                                .id(t.getId())
                                .merchantId(t.getMerchantId())
                                .amount(t.getAmount())
                                .currency(t.getCurrency())
                                .status(t.getStatus().toString())
                                .gatewayUsed(t.getGatewayUsed())
                                .retryCount(t.getRetryCount())
                                .createdAt(t.getCreatedAt())
                                .build()
                ).collect(Collectors.toList());

        return RecentTransactionsResponse.builder()
                .transactions(summaries)
                .build();
    }
}