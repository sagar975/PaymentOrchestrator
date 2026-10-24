package com.payments.orchestrator.adapter.web.webhook;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Verifies webhook signatures from payment gateways.
 * Ensures webhooks are genuinely from the gateway — not forged.
 */
@Service
public class WebhookVerificationService {

    /**
     * Verifies Razorpay webhook signature.
     * Razorpay uses HMAC-SHA256.
     * signature = HMAC-SHA256(payload, webhookSecret)
     *
     * @param payload       raw request body
     * @param signature     X-Razorpay-Signature header value
     * @param webhookSecret Razorpay webhook secret
     * @return true if signature is valid
     */
    public boolean verifyRazorpay(
            String payload,
            String signature,
            String webhookSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifies Stripe webhook signature.
     * Stripe uses HMAC-SHA256 with timestamp.
     * signature = HMAC-SHA256(timestamp + payload, webhookSecret)
     *
     * @param payload       raw request body
     * @param sigHeader     Stripe-Signature header value
     * @param webhookSecret Stripe webhook endpoint secret
     * @return true if signature is valid
     */
    public boolean verifyStripe(
            String payload,
            String sigHeader,
            String webhookSecret) {
        try {
            // Extract timestamp from header
            // Format: t=timestamp,v1=signature
            String timestamp = null;
            String signature = null;

            for (String part : sigHeader.split(",")) {
                if (part.startsWith("t=")) {
                    timestamp = part.substring(2);
                } else if (part.startsWith("v1=")) {
                    signature = part.substring(3);
                }
            }

            if (timestamp == null || signature == null) {
                return false;
            }

            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(
                    signedPayload.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts byte array to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}