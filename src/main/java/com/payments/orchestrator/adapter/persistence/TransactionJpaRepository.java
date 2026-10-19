package com.payments.orchestrator.adapter.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for transaction persistence.
 * <p>
 * Spring auto-generates all SQL at runtime — no implementation needed.
 * Method names follow Spring Data naming convention which are
 * automatically converted to SQL queries.
 */
@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, UUID>{

    /**
     * Finds transaction by idempotency key and merchant.
     * Used to detect duplicate payment requests.
     * Spring Data generates:
     * SELECT * FROM transactions
     * WHERE idempotency_key = ? AND merchant_id = ?
     *
     * @param idempotencyKey unique key per request
     * @param merchantId     the merchant id
     * @return Optional transaction if duplicate found
     */
    Optional<TransactionJpaEntity> findByIdempotencyKeyAndMerchantId(
            String idempotencyKey,
            String merchantId);
}
