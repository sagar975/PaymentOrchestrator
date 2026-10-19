package com.payments.orchestrator.adapter.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Inbound DTO representing a payment transaction request from merchant.
 * <p>
 * Validated before reaching domain layer — domain never sees invalid data.
 * Uses Jakarta Validation annotations for declarative validation.
 */
@Getter
@Setter
@NoArgsConstructor
public class TransactionRequest {
    /**
     * Unique identifier of the merchant initiating the payment.
     */
    @NotBlank(message = "merchantId is required")
    private String merchantId;

    /**
     * Payment amount — must be positive.
     */
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    /**
     * ISO 4217 currency code e.g. INR, USD, EUR.
     */
    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be 3 characters e.g. INR")
    private String currency;

    /**
     * Unique key per request — prevents double charges.
     * Merchant must generate this and keep it the same on retries.
     */
    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;
}
