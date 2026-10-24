package com.payments.orchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Checkout.com gateway integration.
 */
@Configuration
public class CheckoutConfig {

    @Value("${checkout.secret.key}")
    private String secretKey;

    @Value("${checkout.base.url}")
    private String baseUrl;

    @Value("${checkout.processing.channel.id}")
    private String processingChannelId;

    public String getSecretKey() {
        return secretKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getProcessingChannelId() {
        return processingChannelId;
    }
}