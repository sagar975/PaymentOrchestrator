package com.payments.orchestrator.domain.service;

import com.payments.orchestrator.adapter.messaging.EmailService;
import com.payments.orchestrator.adapter.persistence.MfaOtpJpaEntity;
import com.payments.orchestrator.adapter.persistence.MfaOtpRepository;
import com.payments.orchestrator.domain.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MFA service — generates, sends and verifies OTP.
 * Production-grade implementation:
 * - 6-digit cryptographically random OTP
 * - 5 minute expiry
 * - Max 3 attempts before lockout
 * - Old OTPs deleted before new one generated
 * - Async email sending
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private final MfaOtpRepository otpRepository;
    private final EmailService emailService;

    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.max.attempts:3}")
    private int maxAttempts;

    private final SecureRandom secureRandom =
            new SecureRandom();

    /**
     * Generates OTP, stores it and sends to merchant email.
     * Deletes any existing OTPs for this email first.
     */
    @Transactional
    public void generateAndSendOtp(
            String merchantId,
            String email,
            String businessName) {

        log.info("Generating OTP. merchantId={}", merchantId);

        // Delete existing OTPs for this email
        otpRepository.deleteAllByEmail(email);

        // Generate cryptographically random 6-digit OTP
        String otp = String.format("%06d",
                secureRandom.nextInt(1000000));

        // Store OTP with expiry
        MfaOtpJpaEntity entity = new MfaOtpJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail(email);
        entity.setMerchantId(merchantId);
        entity.setOtp(otp);
        entity.setExpiresAt(LocalDateTime.now()
                .plusMinutes(otpExpiryMinutes));
        entity.setUsed(false);
        entity.setAttempts(0);
        entity.setCreatedAt(LocalDateTime.now());

        otpRepository.save(entity);

        // Send OTP email async
        emailService.sendOtpEmail(email, businessName, otp);

        log.info("OTP generated and email queued. email={}",
                email);
    }

    /**
     * Verifies OTP for given email.
     * Enforces max attempts and expiry.
     *
     * @return true if OTP is valid
     * @throws PaymentException if expired, max attempts reached
     */
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        log.info("Verifying OTP. email={}", email);

        MfaOtpJpaEntity entity = otpRepository
                .findValidOtp(email, LocalDateTime.now())
                .orElseThrow(() -> new PaymentException(
                        "OTP_EXPIRED",
                        "OTP has expired or does not exist. " +
                                "Please request a new one."));

        // Check max attempts
        if (entity.getAttempts() >= maxAttempts) {
            log.warn("Max OTP attempts reached. email={}",
                    email);
            throw new PaymentException(
                    "OTP_MAX_ATTEMPTS",
                    "Maximum attempts reached. " +
                            "Please request a new OTP.");
        }

        // Increment attempts
        entity.setAttempts(entity.getAttempts() + 1);
        otpRepository.save(entity);

        // Verify OTP
        if (!entity.getOtp().equals(otp)) {
            int remaining = maxAttempts
                    - entity.getAttempts();
            log.warn("Invalid OTP. email={}, remaining={}",
                    email, remaining);
            throw new PaymentException(
                    "OTP_INVALID",
                    "Invalid OTP. " + remaining
                            + " attempts remaining.");
        }

        // Mark as used
        entity.setUsed(true);
        otpRepository.save(entity);

        log.info("OTP verified successfully. email={}", email);
        return true;
    }
}