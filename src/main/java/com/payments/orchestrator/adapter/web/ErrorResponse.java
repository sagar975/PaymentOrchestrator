package com.payments.orchestrator.adapter.web;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response returned to merchant on any failure.
 * <p>
 * Consistent format regardless of error type — merchant can
 * always parse the same structure.
 */
@Getter
@Builder
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final Map<String, String> details;
}
