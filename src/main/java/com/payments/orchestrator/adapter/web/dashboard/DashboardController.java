package com.payments.orchestrator.adapter.web.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Dashboard REST controller.
 * Authenticated merchants see only their own data.
 * Admin (null merchantId from token) sees all data.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Extracts merchantId from JWT token.
     * Returns null for admin — sees all merchants.
     */
    private String getMerchantIdFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            String principal = auth.getPrincipal().toString();
            if (principal.startsWith("merchant-")) {
                return principal;
            }
        }
        return null;
    }

    /**
     * Returns overall platform summary stats.
     */
    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary(@RequestParam(required = false) String gateway, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        String merchantId = getMerchantIdFromToken();
        log.info("Dashboard summary. merchantId={}, gateway={}", merchantId, gateway);

        return ResponseEntity.ok(dashboardService.getSummary(merchantId, gateway, from, to));
    }

    /**
     * Returns per-gateway performance metrics.
     */
    @GetMapping("/gateway-performance")
    public ResponseEntity<GatewayPerformanceResponse> getGatewayPerformance(@RequestParam(required = false) String gateway, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        String merchantId = getMerchantIdFromToken();
        log.info("Gateway performance. merchantId={}", merchantId);

        return ResponseEntity.ok(dashboardService.getGatewayPerformance(merchantId, gateway, from, to));
    }

    /**
     * Returns transaction count by status.
     */
    @GetMapping("/status-breakdown")
    public ResponseEntity<StatusBreakdownResponse> getStatusBreakdown(@RequestParam(required = false) String gateway, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        String merchantId = getMerchantIdFromToken();
        log.info("Status breakdown. merchantId={}", merchantId);

        return ResponseEntity.ok(dashboardService.getStatusBreakdown(merchantId, gateway, from, to));
    }

    /**
     * Returns most recent transactions.
     */
    @GetMapping("/recent-transactions")
    public ResponseEntity<RecentTransactionsResponse> getRecentTransactions(@RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String gateway, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        String merchantId = getMerchantIdFromToken();
        log.info("Recent transactions. merchantId={}, limit={}", merchantId, limit);

        return ResponseEntity.ok(dashboardService.getRecentTransactions(limit, merchantId, gateway, from, to));
    }
}