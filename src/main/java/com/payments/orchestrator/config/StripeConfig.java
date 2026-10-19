package com.payments.orchestrator.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Stripe gateway integration.
 * Initializes Stripe SDK with secret key on startup.
 */
@Configuration
public class StripeConfig {

    @Value("${stripe.secret.key}")
    private String secretKey;

    /**
     * Initializes Stripe SDK with secret key.
     * Called automatically after Spring creates this bean.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String getSecretKey() {
        return secretKey;
    }
}