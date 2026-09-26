package com.payments.orchestrator.adapter.persistence;

import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.model.TransactionEvent;
import com.payments.orchestrator.domain.port.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing TransactionRepositoryPort using Spring Data JPA.
 * <p>
 * Bridges the domain layer and persistence layer:
 * - Converts domain Transaction → TransactionJpaEntity (before save)
 * - Converts TransactionJpaEntity → domain Transaction (after load)
 * <p>
 * The domain layer never knows JPA exists — it only sees
 * TransactionRepositoryPort interface.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository transactionRepository;
    private final TransactionEventJpaRepository eventRepository;

    @Override
    public Transaction save(Transaction transaction) {
        log.debug("Saving transaction. id={}, status={}",
                transaction.getId(), transaction.getStatus());
        TransactionJpaEntity entity = toEntity(transaction);
        TransactionJpaEntity saved = transactionRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        log.debug("Finding transaction by id={}", id);
        return transactionRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKeyAndMerchantId(
            String idempotencyKey, String merchantId) {
        log.debug("Checking idempotency. key={}, merchantId={}",
                idempotencyKey, merchantId);
        return transactionRepository
                .findByIdempotencyKeyAndMerchantId(idempotencyKey, merchantId)
                .map(this::toDomain);
    }

    @Override
    public TransactionEvent saveEvent(TransactionEvent event) {
        log.debug("Saving transaction event. transactionId={}, to={}",
                event.getTransactionId(), event.getToStatus());
        TransactionEventJpaEntity entity = toEventEntity(event);
        TransactionEventJpaEntity saved = eventRepository.save(entity);
        return toEventDomain(saved);
    }

    @Override
    public List<TransactionEvent> findEventsByTransactionId(UUID transactionId) {
        log.debug("Loading audit trail. transactionId={}", transactionId);
        return eventRepository
                .findByTransactionIdOrderByOccurredAtAsc(transactionId)
                .stream()
                .map(this::toEventDomain)
                .toList();
    }

    // ─── Mappers — Transaction ───────────────────────────────────────────────

    /**
     * Converts domain Transaction to JPA entity for persistence.
     */
    private TransactionJpaEntity toEntity(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(transaction.getId());
        entity.setMerchantId(transaction.getMerchantId());
        entity.setAmount(transaction.getAmount());
        entity.setCurrency(transaction.getCurrency());
        entity.setStatus(transaction.getStatus());
        entity.setGatewayUsed(transaction.getGatewayUsed());
        entity.setIdempotencyKey(transaction.getIdempotencyKey());
        entity.setRetryCount(transaction.getRetryCount());
        entity.setCreatedAt(transaction.getCreatedAt());
        entity.setUpdatedAt(transaction.getUpdatedAt());
        return entity;
    }

    /**
     * Converts JPA entity back to domain Transaction after loading from DB.
     */
    private Transaction toDomain(TransactionJpaEntity entity) {
        return Transaction.reconstitute(
                entity.getId(),
                entity.getMerchantId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getGatewayUsed(),
                entity.getIdempotencyKey(),
                entity.getRetryCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // ─── Mappers — TransactionEvent ──────────────────────────────────────────

    /**
     * Converts domain TransactionEvent to JPA entity.
     */
    private TransactionEventJpaEntity toEventEntity(TransactionEvent event) {
        TransactionEventJpaEntity entity = new TransactionEventJpaEntity();
        entity.setId(event.getId());
        entity.setTransactionId(event.getTransactionId());
        entity.setFromStatus(event.getFromStatus());
        entity.setToStatus(event.getToStatus());
        entity.setGatewayResponse(event.getGatewayResponse());
        entity.setDescription(event.getDescription());
        entity.setOccurredAt(event.getOccurredAt());
        return entity;
    }

    /**
     * Converts JPA entity back to domain TransactionEvent.
     */
    private TransactionEvent toEventDomain(TransactionEventJpaEntity entity) {
        return new TransactionEvent(
                entity.getTransactionId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getGatewayResponse(),
                entity.getDescription()
        );
    }
}