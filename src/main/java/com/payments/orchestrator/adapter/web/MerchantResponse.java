package com.payments.orchestrator.adapter.web;

import com.payments.orchestrator.domain.model.Merchant;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for merchant operations.
 */
@Getter
@Builder
public class MerchantResponse {

    private final UUID id;
    private final String merchantId;
    private final String businessName;
    private final String email;
    private final String country;
    private final String currency;
    private final String status;
    private final LocalDateTime createdAt;

    public static MerchantResponse from(Merchant merchant) {
        return MerchantResponse.builder().id(merchant.getId()).merchantId(merchant.getMerchantId()).businessName(merchant.getBusinessName()).email(merchant.getEmail()).country(merchant.getCountry()).currency(merchant.getCurrency()).status(merchant.getStatus()).createdAt(merchant.getCreatedAt()).build();
    }
}