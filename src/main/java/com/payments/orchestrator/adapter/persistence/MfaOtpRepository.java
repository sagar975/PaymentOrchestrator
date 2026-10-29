package com.payments.orchestrator.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MFA OTP operations.
 */
@Repository
public interface MfaOtpRepository
        extends JpaRepository<MfaOtpJpaEntity, UUID> {

    /**
     * Find latest valid OTP for email.
     */
    @Query("SELECT o FROM MfaOtpJpaEntity o " +
            "WHERE o.email = :email " +
            "AND o.used = false " +
            "AND o.expiresAt > :now " +
            "ORDER BY o.createdAt DESC")
    Optional<MfaOtpJpaEntity> findValidOtp(
            @Param("email") String email,
            @Param("now") LocalDateTime now);

    /**
     * Delete all OTPs for email — cleanup before generating new one.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MfaOtpJpaEntity o " +
            "WHERE o.email = :email")
    void deleteAllByEmail(@Param("email") String email);

    /**
     * Delete expired OTPs — cleanup job.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MfaOtpJpaEntity o " +
            "WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);
}