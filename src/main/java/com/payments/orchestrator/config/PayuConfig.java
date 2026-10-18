package com.payments.orchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for PayU gateway integration.
 * Provides PayU merchant credentials.
 */
@Configuration
public class PayuConfig {

    @Value("${payu.merchant.key}")
    private String merchantKey;

    @Value("${payu.merchant.salt}")
    private String merchantSalt;

    @Value("${payu.base.url}")
    private String baseUrl;

    public String getMerchantKey() {
        return merchantKey;
    }

    public String getMerchantSalt() {
        return merchantSalt;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}