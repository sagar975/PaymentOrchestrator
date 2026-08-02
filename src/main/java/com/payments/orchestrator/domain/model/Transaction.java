package com.payments.orchestrator.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core domain object representing a payment transaction.
 * <p>
 * This is a pure Java class with no framework dependencies.
 * JPA mapping is handled in the adapter/persistence layer.
 * <p>
 * Key invariants:
 * - idempotencyKey must be unique per merchant — prevents double charges
 * - status transitions must follow the defined state machine
 * - retryCount cannot exceed MAX_RETRY_ATTEMPTS
 * - amount must always be positive
 */
public class Transaction {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private UUID id;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private String gatewayUsed;
    private String idempotencyKey;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Private constructor — use factory method create() instead.
     * Ensures every transaction starts in INITIATED state only.
     */
    private Transaction() {
    }

    /**
     * Factory method to create a new transaction.
     * Every transaction starts in INITIATED state.
     *
     * @param merchantId     unique identifier of the merchant
     * @param amount         transaction amount — must be positive
     * @param currency       ISO 4217 currency code e.g. INR, USD
     * @param idempotencyKey unique key per request — prevents double charges
     * @return new Transaction in INITIATED state
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static Transaction create(
            String merchantId,
            BigDecimal amount,
            String currency,
            String idempotencyKey) {

        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId cannot be blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency cannot be blank");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey cannot be blank");
        }

        Transaction transaction = new Transaction();
        transaction.id = UUID.randomUUID();
        transaction.merchantId = merchantId;
        transaction.amount = amount;
        transaction.currency = currency.toUpperCase();
        transaction.status = TransactionStatus.INITIATED;
        transaction.idempotencyKey = idempotencyKey;
        transaction.retryCount = 0;
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    /**
     * Transitions the transaction to a new status.
     * Enforces state machine rules — invalid transitions throw exception.
     *
     * @param newStatus the target status
     * @throws IllegalStateException if transition is not allowed
     */
    public void transitionTo(TransactionStatus newStatus) {
        if (this.status.isTerminal()) {
            throw new IllegalStateException(
                    String.format("Cannot transition from terminal state %s to %s",
                            this.status, newStatus));
        }
        if (!isValidTransition(this.status, newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid transition from %s to %s",
                            this.status, newStatus));
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks transaction as failed and increments retry count.
     * If max retries exceeded, caller should transition to FAILED_FINAL.
     */
    public void markFailed() {
        transitionTo(TransactionStatus.FAILED);
        this.retryCount++;
    }

    /**
     * Returns true if another retry attempt is allowed.
     */
    public boolean canRetry() {
        return this.status.isRetryable()
                && this.retryCount < MAX_RETRY_ATTEMPTS;
    }

    /**
     * Sets the gateway used for this transaction attempt.
     *
     * @param gatewayName name of the payment gateway
     */
    public void assignGateway(String gatewayName) {
        if (gatewayName == null || gatewayName.isBlank()) {
            throw new IllegalArgumentException("gatewayName cannot be blank");
        }
        this.gatewayUsed = gatewayName;
    }

    /**
     * Validates whether a state transition is allowed.
     */
    private boolean isValidTransition(TransactionStatus from, TransactionStatus to) {
        return switch (from) {
            case INITIATED -> to == TransactionStatus.ROUTING;
            case ROUTING -> to == TransactionStatus.AUTHORIZED
                    || to == TransactionStatus.FAILED;
            case AUTHORIZED -> to == TransactionStatus.CAPTURED
                    || to == TransactionStatus.FAILED;
            case CAPTURED -> to == TransactionStatus.SETTLED
                    || to == TransactionStatus.FAILED;
            case FAILED -> to == TransactionStatus.RETRYING
                    || to == TransactionStatus.FAILED_FINAL;
            case RETRYING -> to == TransactionStatus.ROUTING
                    || to == TransactionStatus.FAILED_FINAL;
            default -> false;
        };
    }

    // ─── Getters only — no setters — state changes go through methods above ───

    public UUID getId() {
        return id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getGatewayUsed() {
        return gatewayUsed;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}