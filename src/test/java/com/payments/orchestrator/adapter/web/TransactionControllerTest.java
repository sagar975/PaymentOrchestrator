package com.payments.orchestrator.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orchestrator.domain.exception.PaymentException;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.service.OrchestrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for TransactionController.
 * Uses @WebMvcTest — loads only web layer, mocks service.
 * Tests HTTP behavior: status codes, request validation,
 * response format.
 */
@WebMvcTest(TransactionController.class)
@DisplayName("TransactionController")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrchestrationService orchestrationService;

    private static final String BASE_URL = "/api/v1/transactions";
    private static final String MERCHANT_ID = "merchant-001";
    private static final BigDecimal AMOUNT = new BigDecimal("999.99");
    private static final String CURRENCY = "INR";
    private static final String IDEMPOTENCY_KEY = "order-123";

    @Nested
    @DisplayName("POST /api/v1/transactions")
    class CreateTransaction {

        @Test
        @DisplayName("should return 201 when transaction created successfully")
        void shouldReturn201WhenTransactionCreated() throws Exception {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            when(orchestrationService.initiateTransaction(
                    eq(MERCHANT_ID), eq(AMOUNT),
                    eq(CURRENCY), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(transaction);

            TransactionRequest request = new TransactionRequest();
            request.setMerchantId(MERCHANT_ID);
            request.setAmount(AMOUNT);
            request.setCurrency(CURRENCY);
            request.setIdempotencyKey(IDEMPOTENCY_KEY);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID))
                    .andExpect(jsonPath("$.amount").value(999.99))
                    .andExpect(jsonPath("$.currency").value(CURRENCY))
                    .andExpect(jsonPath("$.status").value("INITIATED"));
        }

        @Test
        @DisplayName("should return 400 when merchantId is missing")
        void shouldReturn400WhenMerchantIdMissing() throws Exception {
            TransactionRequest request = new TransactionRequest();
            request.setAmount(AMOUNT);
            request.setCurrency(CURRENCY);
            request.setIdempotencyKey(IDEMPOTENCY_KEY);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details.merchantId")
                            .value("merchantId is required"));
        }

        @Test
        @DisplayName("should return 400 when amount is zero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            TransactionRequest request = new TransactionRequest();
            request.setMerchantId(MERCHANT_ID);
            request.setAmount(BigDecimal.ZERO);
            request.setCurrency(CURRENCY);
            request.setIdempotencyKey(IDEMPOTENCY_KEY);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details.amount")
                            .value("amount must be greater than 0"));
        }

        @Test
        @DisplayName("should return 400 when currency is wrong length")
        void shouldReturn400WhenCurrencyIsWrongLength() throws Exception {
            TransactionRequest request = new TransactionRequest();
            request.setMerchantId(MERCHANT_ID);
            request.setAmount(AMOUNT);
            request.setCurrency("INVALID");
            request.setIdempotencyKey(IDEMPOTENCY_KEY);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details.currency")
                            .value("currency must be 3 characters e.g. INR"));
        }

        @Test
        @DisplayName("should return 400 when body is empty")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details.merchantId").exists())
                    .andExpect(jsonPath("$.details.amount").exists())
                    .andExpect(jsonPath("$.details.currency").exists())
                    .andExpect(jsonPath("$.details.idempotencyKey").exists());
        }

        @Test
        @DisplayName("should return 400 when body is malformed JSON")
        void shouldReturn400WhenBodyIsMalformed() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"merchantId\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{id}")
    class GetTransaction {

        @Test
        @DisplayName("should return 200 when transaction found")
        void shouldReturn200WhenTransactionFound() throws Exception {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            when(orchestrationService.getTransaction(transaction.getId()))
                    .thenReturn(transaction);

            mockMvc.perform(get(BASE_URL + "/" + transaction.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id")
                            .value(transaction.getId().toString()))
                    .andExpect(jsonPath("$.status").value("INITIATED"));
        }

        @Test
        @DisplayName("should return 400 when transaction not found")
        void shouldReturn400WhenTransactionNotFound() throws Exception {
            UUID randomId = UUID.randomUUID();

            when(orchestrationService.getTransaction(randomId))
                    .thenThrow(new PaymentException(
                            "TRANSACTION_NOT_FOUND",
                            "Transaction not found: " + randomId));

            mockMvc.perform(get(BASE_URL + "/" + randomId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error")
                            .value("TRANSACTION_NOT_FOUND"))
                    .andExpect(jsonPath("$.message")
                            .value("Transaction not found: " + randomId));
        }

        @Test
        @DisplayName("should return 400 when id is not a valid UUID")
        void shouldReturn400WhenIdIsNotUuid() throws Exception {
            mockMvc.perform(get(BASE_URL + "/not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
        }
    }
}