package com.payments.orchestrator.domain.exception;

/**
 * Thrown when a payment gateway returns an error or is unreachable.
 * <p>
 * Contains gateway-specific details to support retry decisions
 * and merchant-facing error messages.
 */
public class GatewayException extends PaymentException {

    private final String gatewayName;
    private final boolean retryable;

    /**
     * @param gatewayName name of the gateway that failed e.g. "razorpay"
     * @param errorCode   gateway error code
     * @param message     human readable description
     * @param retryable   true if retrying on another gateway may succeed
     */
    public GatewayException(String gatewayName, String errorCode, String message, boolean retryable) {
        super(errorCode, message);
        if (gatewayName == null || gatewayName.isBlank()) {
            throw new IllegalArgumentException("gateway name can not be blank or null");
        }
        this.gatewayName = gatewayName;
        this.retryable = retryable;

    }

    /**
     * @param gatewayName name of the gateway that failed
     * @param errorCode   gateway error code
     * @param message     human readable description
     * @param retryable   true if retrying on another gateway may succeed
     * @param cause       original exception from gateway client
     */
    public GatewayException(String gatewayName, String errorCode, String message, boolean retryable, Throwable cause) {

        super(errorCode, message, cause);
        if (gatewayName == null || gatewayName.isBlank()) {
            throw new IllegalArgumentException("gateway name can not be blank or null");
        }
        this.gatewayName = gatewayName;
        this.retryable = retryable;

    }

    public String getGatewayName() {
        return gatewayName;
    }

    public boolean isRetryable() {
        return retryable;
    }

}
