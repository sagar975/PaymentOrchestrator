package com.payments.orchestrator.adapter.web;

import com.payments.orchestrator.config.JwtUtil;
import com.payments.orchestrator.domain.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 * Handles merchant login and token generation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MerchantService merchantService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticates merchant and returns JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        log.info("Login attempt. email={}", request.getEmail());

        // Find merchant by email
        return merchantService.findByEmail(request.getEmail()).filter(merchant -> passwordEncoder.matches(request.getPassword(), merchant.getPassword())).map(merchant -> {
            String token = jwtUtil.generateToken(merchant.getMerchantId(), merchant.getEmail(), "MERCHANT");

            log.info("Login successful. merchantId={}", merchant.getMerchantId());

            return ResponseEntity.ok(LoginResponse.builder().token(token).merchantId(merchant.getMerchantId()).businessName(merchant.getBusinessName()).email(merchant.getEmail()).role("MERCHANT").expiresIn(86400000).build());
        }).orElseGet(() -> {
            log.warn("Login failed. email={}", request.getEmail());
            return ResponseEntity.status(401).build();
        });
    }
}