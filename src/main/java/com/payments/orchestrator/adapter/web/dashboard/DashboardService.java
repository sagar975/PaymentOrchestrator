package com.payments.orchestrator.adapter.web.dashboard;

import com.payments.orchestrator.adapter.persistence.TransactionJpaEntity;
import com.payments.orchestrator.domain.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for dashboard analytics.
 * All methods support merchantId, date range and gateway filtering.
 * merchantId = null means admin view — all merchants combined.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final MerchantService merchantService;

    /**
     * Resolves date range — defaults to all time if not provided.
     */
    private LocalDateTime resolveFrom(LocalDateTime from) {
        return from != null ? from : LocalDateTime.of(2000, 1, 1, 0, 0);
    }

    private LocalDateTime resolveTo(LocalDateTime to) {
        return to != null ? to : LocalDateTime.now();
    }

    /**
     * Returns overall platform summary stats.
     * merchantId = null → all merchants (admin view)
     */
    @Transactional(readOnly = true)
    public SummaryResponse getSummary(String merchantId, String gateway, LocalDateTime from, LocalDateTime to) {

        log.debug("Getting summary. merchantId={}, gateway={}", merchantId, gateway);

        List<Object[]> statusCounts = dashboardRepository.countByStatusFiltered(merchantId, gateway, resolveFrom(from), resolveTo(to));

        long total = 0;
        long successful = 0;
        long failed = 0;
        long retried = 0;

        for (Object[] row : statusCounts) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            total += count;

            if (status.equals("AUTHORIZED") || status.equals("CAPTURED") || status.equals("SETTLED")) {
                successful += count;
            }
            if (status.equals("FAILED") || status.equals("FAILED_FINAL")) {
                failed += count;
            }
            if (status.equals("RETRYING")) {
                retried += count;
            }
        }

        double successRate = total > 0 ? Math.round((double) successful / total * 10000.0) / 100.0 : 0.0;

        BigDecimal totalVolume = dashboardRepository.getTotalVolumeFiltered(merchantId, gateway, resolveFrom(from), resolveTo(to));

        return SummaryResponse.builder().totalTransactions(total).successfulTransactions(successful).failedTransactions(failed).successRate(successRate).totalVolume(totalVolume != null ? totalVolume : BigDecimal.ZERO).retriedTransactions(retried).kafkaRetriesSuccessful(0L).build();
    }

    /**
     * Returns per-gateway performance metrics.
     */
    @Transactional(readOnly = true)
    public GatewayPerformanceResponse getGatewayPerformance(String merchantId, String gateway, LocalDateTime from, LocalDateTime to) {

        List<Object[]> stats = dashboardRepository.getGatewayStatsFiltered(merchantId, gateway, resolveFrom(from), resolveTo(to));

        List<GatewayPerformanceResponse.GatewayStats> gatewayStats = stats.stream().map(row -> {
            String gw = row[0].toString();
            long total = ((Number) row[1]).longValue();
            BigDecimal volume = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            long successful = ((Number) row[3]).longValue();
            long failedCount = total - successful;
            double rate = total > 0 ? Math.round((double) successful / total * 10000.0) / 100.0 : 0.0;

            return GatewayPerformanceResponse.GatewayStats.builder().gatewayName(gw).totalTransactions(total).successfulTransactions(successful).failedTransactions(failedCount).successRate(rate).totalVolume(volume).build();
        }).collect(Collectors.toList());

        return GatewayPerformanceResponse.builder().gateways(gatewayStats).build();
    }

    /**
     * Returns transaction count by status.
     */
    @Transactional(readOnly = true)
    public StatusBreakdownResponse getStatusBreakdown(String merchantId, String gateway, LocalDateTime from, LocalDateTime to) {

        List<Object[]> statusCounts = dashboardRepository.countByStatusFiltered(merchantId, gateway, resolveFrom(from), resolveTo(to));

        Map<String, Long> counts = new HashMap<>();
        long total = 0;

        for (Object[] row : statusCounts) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            counts.put(status, count);
            total += count;
        }

        return StatusBreakdownResponse.builder().initiated(counts.getOrDefault("INITIATED", 0L)).routing(counts.getOrDefault("ROUTING", 0L)).authorized(counts.getOrDefault("AUTHORIZED", 0L)).captured(counts.getOrDefault("CAPTURED", 0L)).settled(counts.getOrDefault("SETTLED", 0L)).failed(counts.getOrDefault("FAILED", 0L)).retrying(counts.getOrDefault("RETRYING", 0L)).failedFinal(counts.getOrDefault("FAILED_FINAL", 0L)).total(total).build();
    }

    /**
     * Returns most recent transactions.
     */
    @Transactional(readOnly = true)
    public RecentTransactionsResponse getRecentTransactions(int limit, String merchantId, String gateway, LocalDateTime from, LocalDateTime to) {

        List<TransactionJpaEntity> recent = dashboardRepository.findRecentFiltered(merchantId, gateway, resolveFrom(from), resolveTo(to), PageRequest.of(0, Math.min(limit, 50)));

        List<RecentTransactionsResponse.TransactionSummary> summaries = recent.stream().map(t -> RecentTransactionsResponse.TransactionSummary.builder().id(t.getId()).merchantId(t.getMerchantId()).merchantName(merchantService.getDisplayName(t.getMerchantId())).amount(t.getAmount()).currency(t.getCurrency()).status(t.getStatus().toString()).gatewayUsed(t.getGatewayUsed()).retryCount(t.getRetryCount()).createdAt(t.getCreatedAt()).build()).collect(Collectors.toList());

        return RecentTransactionsResponse.builder().transactions(summaries).build();
    }
}