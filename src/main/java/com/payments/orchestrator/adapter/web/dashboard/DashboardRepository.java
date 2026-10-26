package com.payments.orchestrator.adapter.web.dashboard;

import com.payments.orchestrator.adapter.persistence.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for dashboard analytics queries.
 */
@Repository
public interface DashboardRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    /**
     * Count transactions by status.
     */
    @Query("SELECT t.status, COUNT(t) FROM TransactionJpaEntity t " + "GROUP BY t.status")
    List<Object[]> countByStatus();

    /**
     * Count and sum transactions by gateway.
     */
    @Query("SELECT t.gatewayUsed, COUNT(t), SUM(t.amount), " + "SUM(CASE WHEN t.status = 'AUTHORIZED' OR " + "t.status = 'CAPTURED' OR t.status = 'SETTLED' " + "THEN 1 ELSE 0 END) " + "FROM TransactionJpaEntity t " + "WHERE t.gatewayUsed IS NOT NULL " + "GROUP BY t.gatewayUsed")
    List<Object[]> getGatewayStats();

    /**
     * Get most recent transactions ordered by creation time.
     */
    @Query("SELECT t FROM TransactionJpaEntity t " + "ORDER BY t.createdAt DESC")
    List<TransactionJpaEntity> findRecentTransactions(org.springframework.data.domain.Pageable pageable);
}