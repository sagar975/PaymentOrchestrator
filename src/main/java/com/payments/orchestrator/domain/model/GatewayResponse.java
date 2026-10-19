package com.payments.orchestrator.domain.model;

/**
 * Represents the normalized response from any payment gateway.
 * <p>
 * Each gateway returns different response formats — Razorpay, Stripe,
 * PayU all have different field names and structures. Each adapter
 * normalizes its gateway-specific response into this common model.
 * <p>
 * The domain layer only ever sees GatewayResponse — never
 * gateway-specific response objects.
 */
public class GatewayResponse {

    private final boolean success;
    private final String gatewayTransactionId;
    private final String errorCode;
    private final String errorMessage;
    private final String rawResponse;

    private GatewayResponse(Builder builder) {
        this.success = builder.success;
        this.gatewayTransactionId = builder.gatewayTransactionId;
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
        this.rawResponse = builder.rawResponse;
    }
    /**
     * Creates a successful gateway response.
     *
     * @param gatewayTransactionId gateway's own transaction reference
     * @param rawResponse          raw response string from gateway
     * @return successful GatewayResponse
     */
    public static GatewayResponse success(
            String gatewayTransactionId,
            String rawResponse) {
        return new Builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .rawResponse(rawResponse)
                .build();
    }

    /**
     * Creates a failed gateway response.
     *
     * @param errorCode    gateway error code
     * @param errorMessage human readable error description
     * @param rawResponse  raw response string from gateway
     * @return failed GatewayResponse
     */
    public static GatewayResponse failure(
            String errorCode,
            String errorMessage,
            String rawResponse) {
        return new Builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .rawResponse(rawResponse)
                .build();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * Builder for GatewayResponse.
     * Used internally via factory methods success() and failure().
     */
    public static class Builder {
        private boolean success;
        private String gatewayTransactionId;
        private String errorCode;
        private String errorMessage;
        private String rawResponse;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder gatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder rawResponse(String rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        public GatewayResponse build() {
            return new GatewayResponse(this);
        }
    }
}