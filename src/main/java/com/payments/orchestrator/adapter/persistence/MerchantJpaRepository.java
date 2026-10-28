package com.payments.orchestrator.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for merchants.
 */
@Repository
public interface MerchantJpaRepository extends JpaRepository<MerchantJpaEntity, UUID> {

    Optional<MerchantJpaEntity> findByMerchantId(String merchantId);

    Optional<MerchantJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMerchantId(String merchantId);
}