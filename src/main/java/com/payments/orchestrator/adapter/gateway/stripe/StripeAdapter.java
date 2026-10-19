package com.payments.orchestrator.adapter.gateway.stripe;

import com.payments.orchestrator.domain.exception.GatewayException;
import com.payments.orchestrator.domain.model.GatewayResponse;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.port.GatewayPort;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.model.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Stripe gateway adapter implementing GatewayPort.
 * <p>
 * Covers global payments — US, UK, EU, UAE, Singapore.
 * Uses Stripe Java SDK for API calls.
 * <p>
 * Stripe uses PaymentIntents — two step process:
 * 1. Create PaymentIntent (authorize — reserves funds)
 * 2. Capture PaymentIntent (capture — debits customer)
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class StripeAdapter implements GatewayPort {

    private static final String GATEWAY_NAME = "stripe";

    @Override
    public String getGatewayName() {
        return GATEWAY_NAME;
    }

    /**
     * Creates a Stripe PaymentIntent — authorizes payment.
     * Reserves funds without charging the customer yet.
     *
     * @param transaction the transaction to authorize
     * @return GatewayResponse with PaymentIntent details
     * @throws GatewayException if Stripe rejects or unreachable
     */
    @Override
    public GatewayResponse authorize(Transaction transaction) {
        log.info("Authorizing transaction via Stripe. txnId={}, amount={}",
                transaction.getId(), transaction.getAmount());

        try {
            // Stripe amount is in smallest currency unit
            // $9.99 = 999 cents, ₹999 = 99900 paise
            long amountInSmallestUnit = transaction.getAmount()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(amountInSmallestUnit)
                            .setCurrency(transaction.getCurrency()
                                    .toLowerCase())
                            .setCaptureMethod(
                                    PaymentIntentCreateParams
                                            .CaptureMethod.MANUAL)
                            .setConfirm(false)
                            .putMetadata("transactionId",
                                    transaction.getId().toString())
                            .putMetadata("merchantId",
                                    transaction.getMerchantId())
                            .putMetadata("idempotencyKey",
                                    transaction.getIdempotencyKey())
                            .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            log.info("Stripe PaymentIntent created. id={}, status={}, txnId={}",
                    paymentIntent.getId(),
                    paymentIntent.getStatus(),
                    transaction.getId());

            return GatewayResponse.success(
                    paymentIntent.getId(),
                    paymentIntent.toJson());

        } catch (StripeException e) {
            log.error("Stripe error. code={}, txnId={}",
                    e.getCode(), transaction.getId(), e);

            boolean retryable = isRetryable(e);

            throw new GatewayException(
                    GATEWAY_NAME,
                    e.getCode() != null ? e.getCode() : "STRIPE_ERROR",
                    e.getMessage(),
                    retryable,
                    e);
        }
    }

    /**
     * Captures a Stripe PaymentIntent.
     * Actually debits the customer after authorization.
     *
     * @param transaction the authorized transaction to capture
     * @return GatewayResponse with capture result
     */
    @Override
    public GatewayResponse capture(Transaction transaction) {
        log.info("Capturing Stripe payment. txnId={}",
                transaction.getId());

        try {
            PaymentIntent paymentIntent = PaymentIntent
                    .retrieve(transaction.getGatewayUsed());

            PaymentIntentCaptureParams params =
                    PaymentIntentCaptureParams.builder().build();

            PaymentIntent captured = paymentIntent.capture(params);

            log.info("Stripe payment captured. id={}, txnId={}",
                    captured.getId(), transaction.getId());

            return GatewayResponse.success(
                    captured.getId(), captured.toJson());

        } catch (StripeException e) {
            log.error("Stripe capture error. txnId={}",
                    transaction.getId(), e);
            throw new GatewayException(
                    GATEWAY_NAME,
                    e.getCode() != null ? e.getCode() : "CAPTURE_ERROR",
                    e.getMessage(),
                    false,
                    e);
        }
    }

    /**
     * Refunds a Stripe payment.
     *
     * @param transaction the captured transaction to refund
     * @return GatewayResponse with refund result
     */
    @Override
    public GatewayResponse refund(Transaction transaction) {
        log.info("Refunding Stripe payment. txnId={}",
                transaction.getId());

        try {
            long amountInSmallestUnit = transaction.getAmount()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(transaction.getGatewayUsed())
                    .setAmount(amountInSmallestUnit)
                    .build();

            Refund refund = Refund.create(params);

            log.info("Stripe refund created. id={}, txnId={}",
                    refund.getId(), transaction.getId());

            return GatewayResponse.success(
                    refund.getId(), refund.toJson());

        } catch (StripeException e) {
            log.error("Stripe refund error. txnId={}",
                    transaction.getId(), e);
            throw new GatewayException(
                    GATEWAY_NAME,
                    e.getCode() != null ? e.getCode() : "REFUND_ERROR",
                    e.getMessage(),
                    false,
                    e);
        }
    }

    /**
     * Determines if a Stripe error is worth retrying.
     * Network errors and rate limits are retryable.
     * Card declines and validation errors are not.
     */
    private boolean isRetryable(StripeException e) {
        if (e.getCode() == null) return true;

        return switch (e.getCode()) {
            case "rate_limit",
                 "api_connection_error",
                 "api_error" -> true;
            case "card_declined",
                 "invalid_card",
                 "insufficient_funds",
                 "invalid_request_error" -> false;
            default -> false;
        };
    }
}