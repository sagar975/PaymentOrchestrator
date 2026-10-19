package com.payments.orchestrator.adapter.messaging;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published to Kafka when a gateway fails.
 * <p>
 * Consumed by RetryConsumer to attempt authorization
 * on a fallback gateway.
 * <p>
 * No-arg constructor required for Jackson deserialization.
 */
public class TransactionFailedEvent {

    private UUID transactionId;
    private String failedGateway;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
    private int retryCount;

    /**
     * No-arg constructor required by Jackson for deserialization.
     */
    public TransactionFailedEvent() {
    }

    /**
     * Creates a new TransactionFailedEvent.
     *
     * @param transactionId  UUID of the failed transaction
     * @param failedGateway  name of the gateway that failed
     * @param merchantId     merchant identifier
     * @param amount         transaction amount
     * @param currency       ISO 4217 currency code
     * @param idempotencyKey unique key for this transaction
     * @param retryCount     number of retries attempted so far
     */
    public TransactionFailedEvent(UUID transactionId, String failedGateway, String merchantId, BigDecimal amount, String currency, String idempotencyKey, int retryCount) {
        this.transactionId = transactionId;
        this.failedGateway = failedGateway;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.retryCount = retryCount;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getFailedGateway() {
        return failedGateway;
    }

    public void setFailedGateway(String failedGateway) {
        this.failedGateway = failedGateway;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}