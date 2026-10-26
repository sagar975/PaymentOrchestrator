package com.payments.orchestrator.adapter.web.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard REST controller.
 * Provides analytics and metrics for the payment platform.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Returns overall platform summary stats.
     */
    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary() {
        log.info("Dashboard summary requested");
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    /**
     * Returns per-gateway performance metrics.
     */
    @GetMapping("/gateway-performance")
    public ResponseEntity<GatewayPerformanceResponse> getGatewayPerformance() {
        log.info("Gateway performance requested");
        return ResponseEntity.ok(dashboardService.getGatewayPerformance());
    }

    /**
     * Returns transaction count by status.
     */
    @GetMapping("/status-breakdown")
    public ResponseEntity<StatusBreakdownResponse> getStatusBreakdown() {
        log.info("Status breakdown requested");
        return ResponseEntity.ok(dashboardService.getStatusBreakdown());
    }

    /**
     * Returns most recent transactions.
     */
    @GetMapping("/recent-transactions")
    public ResponseEntity<RecentTransactionsResponse> getRecentTransactions(@RequestParam(defaultValue = "10") int limit) {
        log.info("Recent transactions requested. limit={}", limit);
        return ResponseEntity.ok(dashboardService.getRecentTransactions(limit));
    }
}