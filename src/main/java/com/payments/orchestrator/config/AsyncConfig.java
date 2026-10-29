package com.payments.orchestrator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables @Async for email sending.
 * Email is sent in background thread — does not block request.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}