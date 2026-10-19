package com.payments.orchestrator.domain.port;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.model.TransactionEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port defining how transactions are persisted and retrieved.
 * <p>
 * The domain layer depends on this interface — never on JPA or any
 * specific database technology. The actual implementation lives in
 * adapter/persistence/ using Spring Data JPA.
 * <p>
 * This separation means the database can be swapped (Postgres → MongoDB)
 * without touching any domain logic.
 */

public interface TransactionRepositoryPort {

    /**
     * Persists a new transaction or updates an existing one.
     *
     * @param transaction the transaction to save
     * @return the saved transaction
     */
    Transaction save(Transaction transaction);

    /**
     * Finds a transaction by its unique id.
     *
     * @param id the transaction UUID
     * @return Optional containing transaction if found, empty otherwise
     */
    Optional<Transaction> findById(UUID id);

    /**
     * Finds a transaction by idempotency key and merchant.
     * Used to detect duplicate requests and prevent double charges.
     *
     * @param idempotencyKey the idempotency key
     * @param merchantId     the merchant id
     * @return Optional containing transaction if found, empty otherwise
     */
    Optional<Transaction> findByIdempotencyKeyAndMerchantId(
            String idempotencyKey,
            String merchantId);

    /**
     * Saves a transaction audit event.
     *
     * @param event the event to persist
     * @return the saved event
     */
    TransactionEvent saveEvent(TransactionEvent event);

    /**
     * Retrieves all events for a transaction in chronological order.
     * Used for audit trail and dispute resolution.
     *
     * @param transactionId the transaction UUID
     * @return list of events ordered by occurredAt ascending
     */
    List<TransactionEvent> findEventsByTransactionId(UUID transactionId);
}
