package com.payments.orchestrator.adapter.gateway.razorpay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Request DTO for capturing a Razorpay payment.
 */
@Getter
@AllArgsConstructor
public class RazorpayCaptureRequest {

    @JsonProperty("amount")
    private final long amount;

    @JsonProperty("currency")
    private final String currency;
}