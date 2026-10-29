package com.payments.orchestrator.adapter.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for MFA OTP storage.
 * Stores OTP with expiry, usage status and attempt count.
 */
@Entity
@Table(name = "mfa_otps")
@Getter
@Setter
@NoArgsConstructor
public class MfaOtpJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "otp", nullable = false, length = 6)
    private String otp;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}