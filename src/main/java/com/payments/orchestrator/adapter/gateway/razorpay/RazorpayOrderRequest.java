package com.payments.orchestrator.adapter.gateway.razorpay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Request DTO for creating a Razorpay order.
 */
@Getter
@AllArgsConstructor
public class RazorpayOrderRequest {

    @JsonProperty("amount")
    private final long amount;

    @JsonProperty("currency")
    private final String currency;

    @JsonProperty("receipt")
    private final String receipt;
}