package com.payments.orchestrator.domain.service;

import com.payments.orchestrator.domain.exception.GatewayException;
import com.payments.orchestrator.domain.exception.PaymentException;
import com.payments.orchestrator.domain.model.GatewayResponse;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.model.TransactionEvent;
import com.payments.orchestrator.domain.model.TransactionStatus;
import com.payments.orchestrator.domain.port.GatewayPort;
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
 * 4. Gateway selection via RoutingService
 * 5. Authorization via selected gateway
 * 6. Event recording at every state transition
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestrationService {

    private final TransactionRepositoryPort transactionRepository;
    private final RoutingService routingService;

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
        var existing = transactionRepository.findByIdempotencyKeyAndMerchantId(idempotencyKey, merchantId);

        if (existing.isPresent()) {
            log.info("Duplicate request detected. idempotencyKey={} — " + "returning existing transaction", idempotencyKey);
            return existing.get();
        }

        // Step 2 — create new transaction in domain
        Transaction transaction = Transaction.create(merchantId, amount, currency, idempotencyKey);

        // Step 3 — persist transaction
        Transaction saved = transactionRepository.save(transaction);

        // Step 4 — record creation event
        transactionRepository.saveEvent(new TransactionEvent(saved.getId(), null, TransactionStatus.INITIATED, null, "Transaction initiated successfully"));

        // Step 5 — select gateway via routing engine
        GatewayPort selectedGateway = routingService.selectGateway(saved);

        // Step 6 — transition to ROUTING
        saved.transitionTo(TransactionStatus.ROUTING);
        saved.assignGateway(selectedGateway.getGatewayName());
        saved = transactionRepository.save(saved);

        transactionRepository.saveEvent(new TransactionEvent(saved.getId(), TransactionStatus.INITIATED, TransactionStatus.ROUTING, null, "Routing to " + selectedGateway.getGatewayName()));

        // Step 7 — authorize via selected gateway
        try {
            GatewayResponse response = selectedGateway.authorize(saved);

            if (response.isSuccess()) {
                saved.transitionTo(TransactionStatus.AUTHORIZED);
                saved = transactionRepository.save(saved);

                transactionRepository.saveEvent(new TransactionEvent(saved.getId(), TransactionStatus.ROUTING, TransactionStatus.AUTHORIZED, response.getRawResponse(), "Authorized by " + selectedGateway.getGatewayName()));

                log.info("Transaction authorized successfully. id={}, gateway={}", saved.getId(), selectedGateway.getGatewayName());

            } else {
                saved.markFailed();
                saved = transactionRepository.save(saved);

                transactionRepository.saveEvent(new TransactionEvent(saved.getId(), TransactionStatus.ROUTING, TransactionStatus.FAILED, response.getRawResponse(), "Failed: " + response.getErrorMessage()));

                log.warn("Transaction failed at gateway. id={}, error={}", saved.getId(), response.getErrorMessage());
            }

        } catch (GatewayException e) {
            saved.markFailed();
            saved = transactionRepository.save(saved);

            transactionRepository.saveEvent(new TransactionEvent(saved.getId(), TransactionStatus.ROUTING, TransactionStatus.FAILED, null, "Gateway exception: " + e.getMessage()));

            log.error("Gateway exception during authorization. id={}", saved.getId(), e);
        }

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