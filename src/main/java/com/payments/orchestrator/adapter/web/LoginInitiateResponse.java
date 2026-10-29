package com.payments.orchestrator.adapter.web;

import lombok.Builder;
import lombok.Getter;

/**
 * Response after step 1 of login.
 * OTP has been sent — merchant must verify it.
 */
@Getter
@Builder
public class LoginInitiateResponse {

    private final String status;
    private final String message;
    private final String email;
    private final String maskedEmail;
}