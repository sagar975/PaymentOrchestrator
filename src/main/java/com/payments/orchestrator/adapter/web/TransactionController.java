package com.payments.orchestrator.adapter.web;

import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.service.OrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing payment transaction endpoints to merchants.
 * <p>
 * All endpoints are versioned under /api/v1/ — breaking changes
 * will introduce /api/v2/ without removing v1.
 * <p>
 * Input validation handled by @Valid — invalid requests never
 * reach the service layer.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final OrchestrationService orchestrationService;

    /**
     * Creates a new payment transaction.
     * <p>
     * Idempotent — calling with same idempotencyKey returns
     * existing transaction without creating a duplicate.
     *
     * @param request validated transaction request from merchant
     * @return 201 Created with transaction details
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {

        log.info("Received transaction request. merchantId={}, amount={}, currency={}", request.getMerchantId(), request.getAmount(), request.getCurrency());

        Transaction transaction = orchestrationService.initiateTransaction(request.getMerchantId(), request.getAmount(), request.getCurrency(), request.getIdempotencyKey());

        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction));
    }

    /**
     * Retrieves a transaction by its unique identifier.
     *
     * @param id transaction UUID
     * @return 200 OK with transaction details
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID id) {

        log.info("Retrieving transaction. id={}", id);

        Transaction transaction = orchestrationService.getTransaction(id);

        return ResponseEntity.status(HttpStatus.OK).body(TransactionResponse.from(transaction));
    }

}
