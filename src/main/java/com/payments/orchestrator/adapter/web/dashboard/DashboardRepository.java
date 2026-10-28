package com.payments.orchestrator.adapter.web.dashboard;

import com.payments.orchestrator.adapter.persistence.TransactionJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for dashboard analytics queries.
 * Supports filtering by merchantId, date range and gateway.
 */
@Repository
public interface DashboardRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    /**
     * Count transactions by status with optional filters.
     */
    @Query("SELECT t.status, COUNT(t) " + "FROM TransactionJpaEntity t " + "WHERE (:merchantId IS NULL " + "   OR t.merchantId = :merchantId) " + "AND (:gateway IS NULL " + "   OR t.gatewayUsed = :gateway) " + "AND t.createdAt BETWEEN :from AND :to " + "GROUP BY t.status")
    List<Object[]> countByStatusFiltered(@Param("merchantId") String merchantId, @Param("gateway") String gateway, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Get gateway stats with optional filters.
     */
    @Query("SELECT t.gatewayUsed, COUNT(t), SUM(t.amount), " + "SUM(CASE WHEN t.status = 'AUTHORIZED' OR " + "t.status = 'CAPTURED' OR t.status = 'SETTLED' " + "THEN 1 ELSE 0 END) " + "FROM TransactionJpaEntity t " + "WHERE t.gatewayUsed IS NOT NULL " + "AND (:merchantId IS NULL " + "   OR t.merchantId = :merchantId) " + "AND (:gateway IS NULL " + "   OR t.gatewayUsed = :gateway) " + "AND t.createdAt BETWEEN :from AND :to " + "GROUP BY t.gatewayUsed")
    List<Object[]> getGatewayStatsFiltered(@Param("merchantId") String merchantId, @Param("gateway") String gateway, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Get total volume with filters.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) " + "FROM TransactionJpaEntity t " + "WHERE (:merchantId IS NULL " + "   OR t.merchantId = :merchantId) " + "AND (:gateway IS NULL " + "   OR t.gatewayUsed = :gateway) " + "AND t.createdAt BETWEEN :from AND :to")
    BigDecimal getTotalVolumeFiltered(@Param("merchantId") String merchantId, @Param("gateway") String gateway, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Get recent transactions with filters.
     */
    @Query("SELECT t FROM TransactionJpaEntity t " + "WHERE (:merchantId IS NULL " + "   OR t.merchantId = :merchantId) " + "AND (:gateway IS NULL " + "   OR t.gatewayUsed = :gateway) " + "AND t.createdAt BETWEEN :from AND :to " + "ORDER BY t.createdAt DESC")
    List<TransactionJpaEntity> findRecentFiltered(@Param("merchantId") String merchantId, @Param("gateway") String gateway, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}