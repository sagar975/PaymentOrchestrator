package com.payments.orchestrator.adapter.web;
import com.payments.orchestrator.domain.exception.GatewayException;
import com.payments.orchestrator.domain.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST endpoints.
 * <p>
 * Catches exceptions thrown anywhere in the request pipeline
 * and converts them to consistent error responses.
 * Merchant always sees the same error format regardless of
 * what went wrong internally.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Handles validation errors — missing or invalid request fields.
     * Returns 400 Bad Request with field-level error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(
                        error.getField(),
                        error.getDefaultMessage()));
        log.warn("Validation failed. errors={}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(400)
                        .error("VALIDATION_FAILED")
                        .message("Request validation failed")
                        .details(errors)
                        .build());
    }

    /**
     * Handles gateway errors — PSP rejected or unreachable.
     * Returns 502 Bad Gateway.
     */
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ErrorResponse> handleGatewayException(
            GatewayException ex) {
        log.error("Gateway error. gateway={}, code={}, retryable={}",
                ex.getGatewayName(), ex.getErrorCode(), ex.isRetryable(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(502)
                        .error(ex.getErrorCode())
                        .message(ex.getMessage())
                        .build());
    }

    /**
     * Handles all other payment domain errors.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex) {
        log.error("Payment error. code={}, message={}",
                ex.getErrorCode(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(400)
                        .error(ex.getErrorCode())
                        .message(ex.getMessage())
                        .build());
    }

    /**
     * Handles unexpected errors — catches anything not handled above.
     * Returns 500 Internal Server Error.
     * Never exposes internal details to merchant.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(500)
                        .error("INTERNAL_ERROR")
                        .message("An unexpected error occurred")
                        .build());
    }
}
