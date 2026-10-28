package com.payments.orchestrator.adapter.web;

import com.payments.orchestrator.domain.model.Merchant;
import com.payments.orchestrator.domain.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for merchant registration and management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * Registers a new merchant on the platform.
     */
    @PostMapping("/register")
    public ResponseEntity<MerchantResponse> register(@Valid @RequestBody MerchantRegistrationRequest request) {

        Merchant merchant = merchantService.registerMerchant(request.getBusinessName(), request.getEmail(), request.getCountry(), request.getCurrency(), request.getPassword());    // ← add password

        return ResponseEntity.status(HttpStatus.CREATED).body(MerchantResponse.from(merchant));
    }

    /**
     * Returns merchant details by merchantId.
     */
    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantResponse> getMerchant(@PathVariable String merchantId) {

        return merchantService.findByMerchantId(merchantId).map(m -> ResponseEntity.ok(MerchantResponse.from(m))).orElse(ResponseEntity.notFound().build());
    }
}