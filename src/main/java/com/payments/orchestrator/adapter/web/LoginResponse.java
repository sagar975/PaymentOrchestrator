package com.payments.orchestrator.adapter.web;

import lombok.Builder;
import lombok.Getter;

/**
 * Login response DTO — returns JWT token and merchant info.
 */
@Getter
@Builder
public class LoginResponse {

    private final String token;
    private final String merchantId;
    private final String businessName;
    private final String email;
    private final String role;
    private final long expiresIn;
}