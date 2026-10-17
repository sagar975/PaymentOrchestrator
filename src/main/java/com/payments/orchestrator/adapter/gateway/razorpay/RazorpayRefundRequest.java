package com.payments.orchestrator.adapter.gateway.razorpay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Request DTO for refunding a Razorpay payment.
 */
@Getter
@AllArgsConstructor
public class RazorpayRefundRequest {

    @JsonProperty("amount")
    private final long amount;
}