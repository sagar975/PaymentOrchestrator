package com.payments.orchestrator.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Merchant domain model.
 * Represents a business registered on the platform.
 */
public class Merchant {

    private UUID id;
    private String merchantId;
    private String businessName;
    private String email;
    private String country;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String password;

    private Merchant() {
    }

    /**
     * Creates a new merchant with generated ID and merchantId.
     */
    public static Merchant create(
            String businessName,
            String email,
            String country,
            String currency,
            String encodedPassword) {

        // existing validation...

        Merchant merchant = new Merchant();
        merchant.id = UUID.randomUUID();
        merchant.merchantId = "merchant-"
                + UUID.randomUUID().toString()
                .substring(0, 8);
        merchant.businessName = businessName;
        merchant.email = email;
        merchant.country = country != null ? country : "IN";
        merchant.currency = currency != null ? currency : "INR";
        merchant.password = encodedPassword;
        merchant.status = "ACTIVE";
        merchant.createdAt = LocalDateTime.now();
        merchant.updatedAt = LocalDateTime.now();
        return merchant;
    }

    /**
     * Reconstitutes a merchant from database.
     */
    public static Merchant reconstitute(
            UUID id,
            String merchantId,
            String businessName,
            String email,
            String country,
            String currency,
            String status,
            String password,           // ← add this
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        Merchant merchant = new Merchant();
        merchant.id = id;
        merchant.merchantId = merchantId;
        merchant.businessName = businessName;
        merchant.email = email;
        merchant.country = country;
        merchant.currency = currency;
        merchant.status = status;
        merchant.password = password;  // ← add this
        merchant.createdAt = createdAt;
        merchant.updatedAt = updatedAt;
        return merchant;
    }

    public UUID getId() {
        return id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getEmail() {
        return email;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getPassword() {
        return password;
    }
}