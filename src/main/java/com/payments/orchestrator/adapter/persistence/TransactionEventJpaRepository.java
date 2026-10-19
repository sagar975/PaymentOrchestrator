package com.payments.orchestrator.adapter.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
/**
 * Spring Data JPA repository for transaction event persistence.
 * <p>
 * Provides audit trail retrieval ordered chronologically.
 */
@Repository
public interface TransactionEventJpaRepository  extends JpaRepository<TransactionEventJpaEntity, UUID>{

    /**
     * Retrieves all events for a transaction in chronological order.
     * Spring Data generates:
     * SELECT * FROM transaction_events
     * WHERE transaction_id = ?
     * ORDER BY occurred_at ASC
     *
     * @param transactionId the transaction UUID
     * @return list of events ordered by occurredAt ascending
     */
    List<TransactionEventJpaEntity> findByTransactionIdOrderByOccurredAtAsc(
            UUID transactionId);
}
