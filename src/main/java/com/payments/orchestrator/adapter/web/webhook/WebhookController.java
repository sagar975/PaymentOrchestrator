package com.payments.orchestrator.adapter.web.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles incoming webhook notifications from payment gateways.
 * Each gateway has its own endpoint for signature verification.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final RazorpayWebhookHandler razorpayHandler;
    private final StripeWebhookHandler stripeHandler;
    private final CheckoutWebhookHandler checkoutHandler;
    private final PayuWebhookHandler payuHandler;

    /**
     * Handles Razorpay webhook events.
     * Razorpay sends X-Razorpay-Signature header for verification.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpay(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        log.info("Received Razorpay webhook");
        razorpayHandler.handle(payload, signature);
        return ResponseEntity.ok("OK");
    }

    /**
     * Handles Stripe webhook events.
     * Stripe sends Stripe-Signature header for verification.
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripe(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        log.info("Received Stripe webhook");
        stripeHandler.handle(payload, signature);
        return ResponseEntity.ok("OK");
    }

    /**
     * Handles Checkout.com webhook events.
     * Checkout.com sends Authorization header for verification.
     */
    @PostMapping("/checkout")
    public ResponseEntity<String> handleCheckout(
            @RequestBody String payload,
            @RequestHeader("Authorization") String signature) {

        log.info("Received Checkout.com webhook");
        checkoutHandler.handle(payload, signature);
        return ResponseEntity.ok("OK");
    }

    /**
     * Handles PayU webhook events.
     * PayU sends form-encoded POST data.
     */
    @PostMapping("/payu")
    public ResponseEntity<String> handlePayu(
            @RequestParam Map<String, String> params) {

        log.info("Received PayU webhook");
        payuHandler.handle(params);
        return ResponseEntity.ok("OK");
    }
}