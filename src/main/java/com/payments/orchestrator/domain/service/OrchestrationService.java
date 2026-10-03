package com.payments.orchestrator.domain.service;

import com.payments.orchestrator.domain.exception.PaymentException;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.model.TransactionEvent;
import com.payments.orchestrator.domain.model.TransactionStatus;
import com.payments.orchestrator.domain.port.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Core orchestration service — brain of the payment platform.
 * <p>
 * Coordinates the full payment lifecycle:
 * 1. Idempotency check — prevent double charges
 * 2. Transaction creation
 * 3. Persistence
 * 4. Event recording
 * <p>
 * Gateway routing and retry logic will be added in Phase 3.
 * For now — creates and persists transactions correctly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestrationService {
    private final TransactionRepositoryPort transactionRepository;

    /**
     * Initiates a new payment transaction.
     * <p>
     * Checks idempotency key first — if duplicate request found,
     * returns existing transaction instead of creating new one.
     * This prevents double charges on network retries.
     *
     * @param merchantId     unique merchant identifier
     * @param amount         payment amount — must be positive
     * @param currency       ISO 4217 currency code
     * @param idempotencyKey unique key per request
     * @return created or existing Transaction
     * @throws PaymentException if validation fails
     */
    @Transactional
    public Transaction initiateTransaction(String merchantId, BigDecimal amount, String currency, String idempotencyKey) {

        log.info("Initiating transaction. merchantId={}, amount={}, currency={}", merchantId, amount, currency);

        // Step 1 — check idempotency
        // if same key + merchant already exists return existing transaction
        var existing = transactionRepository.findByIdempotencyKeyAndMerchantId(idempotencyKey, merchantId);

        if (existing.isPresent()) {
            log.info("Duplicate request detected. idempotencyKey={} — " + "returning existing transaction", idempotencyKey);
            return existing.get();
        }

        // Step 2 — create new transaction in domain
        Transaction transaction = Transaction.create(merchantId, amount, currency, idempotencyKey);

        // Step 3 — persist transaction
        Transaction saved = transactionRepository.save(transaction);

        // Step 4 — record audit event
        TransactionEvent event = new TransactionEvent(saved.getId(), null, TransactionStatus.INITIATED, null, "Transaction initiated successfully");
        transactionRepository.saveEvent(event);

        log.info("Transaction created successfully. id={}", saved.getId());
        return saved;
    }

    /**
     * Retrieves a transaction by its unique identifier.
     *
     * @param id transaction UUID
     * @return Transaction if found
     * @throws PaymentException if not found
     */
    @Transactional(readOnly = true)
    public Transaction getTransaction(UUID id) {
        log.debug("Retrieving transaction. id={}", id);
        return transactionRepository.findById(id).orElseThrow(() -> new PaymentException("TRANSACTION_NOT_FOUND", "Transaction not found: " + id));
    }
}
