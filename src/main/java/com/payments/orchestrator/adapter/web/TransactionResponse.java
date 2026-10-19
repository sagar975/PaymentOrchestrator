package com.payments.orchestrator.adapter.web;

import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.model.TransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
/**
 * Outbound DTO representing a payment transaction response to merchant.
 * <p>
 * Built from domain Transaction — merchant never sees internal domain objects.
 * Uses Builder pattern for clean construction.
 */
@Getter
@Builder
public class TransactionResponse {
    private final UUID id;
    private final String merchantId;
    private final BigDecimal amount;
    private final String currency;
    private final TransactionStatus status;
    private final String gatewayUsed;
    private final String idempotencyKey;
    private final int retryCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;


    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .merchantId(transaction.getMerchantId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .gatewayUsed(transaction.getGatewayUsed())
                .idempotencyKey(transaction.getIdempotencyKey())
                .retryCount(transaction.getRetryCount())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
