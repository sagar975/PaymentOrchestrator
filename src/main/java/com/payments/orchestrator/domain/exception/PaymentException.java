package com.payments.orchestrator.domain.exception;

/**
 * Base exception for all payment domain errors.
 * <p>
 * All payment-specific exceptions extend this class,
 * allowing callers to catch all payment errors with
 * a single catch block when needed.
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;

    /**
     * @param errorCode machine-readable error code e.g. "INVALID_AMOUNT"
     * @param message   human readable description
     */
    public PaymentException(String errorCode, String message) {
        super(message);
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorcode can not be blank or null");
        }
        this.errorCode = errorCode;
    }

    /**
     * @param errorCode machine-readable error code e.g. "INVALID_AMOUNT"
     * @param message   human readable description
     */
    public PaymentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorcode can not be blank or null");
        }
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
