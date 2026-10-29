package com.payments.orchestrator.adapter.web;

import com.payments.orchestrator.config.JwtUtil;
import com.payments.orchestrator.domain.service.MerchantService;
import com.payments.orchestrator.domain.service.MfaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 * Two-step login: credentials → OTP → JWT token.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MerchantService merchantService;
    private final MfaService mfaService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Step 1 — Validates credentials and sends OTP.
     * Does NOT return JWT yet.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginInitiateResponse> login(@Valid @RequestBody LoginRequest request) {

        log.info("Login step 1. email={}", request.getEmail());

        return merchantService.findByEmail(request.getEmail()).filter(merchant -> passwordEncoder.matches(request.getPassword(), merchant.getPassword())).map(merchant -> {
            // Send OTP
            mfaService.generateAndSendOtp(merchant.getMerchantId(), merchant.getEmail(), merchant.getBusinessName());

            String maskedEmail = maskEmail(merchant.getEmail());

            log.info("OTP sent. merchantId={}", merchant.getMerchantId());

            return ResponseEntity.ok(LoginInitiateResponse.builder().status("OTP_SENT").message("OTP sent to " + maskedEmail).email(merchant.getEmail()).maskedEmail(maskedEmail).build());
        }).orElseGet(() -> {
            log.warn("Invalid credentials. email={}", request.getEmail());
            return ResponseEntity.status(401).build();
        });
    }

    /**
     * Step 2 — Verifies OTP and returns JWT token.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {

        log.info("Login step 2 — OTP verify. email={}", request.getEmail());

        // Verify OTP — throws PaymentException if invalid
        mfaService.verifyOtp(request.getEmail(), request.getOtp());

        // OTP verified — get merchant and generate JWT
        return merchantService.findByEmail(request.getEmail()).map(merchant -> {
            String token = jwtUtil.generateToken(merchant.getMerchantId(), merchant.getEmail(), "MERCHANT");

            log.info("Login complete. merchantId={}", merchant.getMerchantId());

            return ResponseEntity.ok(LoginResponse.builder().token(token).merchantId(merchant.getMerchantId()).businessName(merchant.getBusinessName()).email(merchant.getEmail()).role("MERCHANT").expiresIn(86400000).build());
        }).orElse(ResponseEntity.status(401).build());
    }

    /**
     * Masks email for display — s****@gmail.com
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        return email.charAt(0) + "****" + email.substring(atIndex);
    }
}