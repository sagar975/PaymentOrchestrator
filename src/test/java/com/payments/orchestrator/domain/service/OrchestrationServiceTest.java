package com.payments.orchestrator.domain.service;

import com.payments.orchestrator.domain.exception.PaymentException;
import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.model.TransactionEvent;
import com.payments.orchestrator.domain.model.TransactionStatus;
import com.payments.orchestrator.domain.port.TransactionRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrchestrationService.
 * Uses Mockito to mock dependencies —
 * no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrchestrationService")
class OrchestrationServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepository;

    @InjectMocks
    private OrchestrationService orchestrationService;

    private static final String MERCHANT_ID = "merchant-001";
    private static final BigDecimal AMOUNT = new BigDecimal("999.99");
    private static final String CURRENCY = "INR";
    private static final String IDEMPOTENCY_KEY = "order-123";

    @Nested
    @DisplayName("initiateTransaction")
    class InitiateTransaction {

        @Test
        @DisplayName("should create and save new transaction")
        void shouldCreateAndSaveNewTransaction() {
            // Arrange
            when(transactionRepository
                    .findByIdempotencyKeyAndMerchantId(
                            IDEMPOTENCY_KEY, MERCHANT_ID))
                    .thenReturn(Optional.empty());

            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            when(transactionRepository.saveEvent(any(TransactionEvent.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            Transaction result = orchestrationService.initiateTransaction(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            // Assert
            assertThat(result.getId()).isNotNull();
            assertThat(result.getMerchantId()).isEqualTo(MERCHANT_ID);
            assertThat(result.getAmount()).isEqualTo(AMOUNT);
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.INITIATED);

            // verify save was called once
            verify(transactionRepository, times(1))
                    .save(any(Transaction.class));

            // verify audit event was saved
            verify(transactionRepository, times(1))
                    .saveEvent(any(TransactionEvent.class));
        }

        @Test
        @DisplayName("should return existing transaction on duplicate request")
        void shouldReturnExistingTransactionOnDuplicateRequest() {
            // Arrange — existing transaction already in DB
            Transaction existing = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            when(transactionRepository
                    .findByIdempotencyKeyAndMerchantId(
                            IDEMPOTENCY_KEY, MERCHANT_ID))
                    .thenReturn(Optional.of(existing));

            // Act
            Transaction result = orchestrationService.initiateTransaction(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            // Assert — same transaction returned
            assertThat(result.getId()).isEqualTo(existing.getId());
            assertThat(result.getIdempotencyKey())
                    .isEqualTo(existing.getIdempotencyKey());

            // verify save was NEVER called — no duplicate created
            verify(transactionRepository, never())
                    .save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("getTransaction")
    class GetTransaction {

        @Test
        @DisplayName("should return transaction when found")
        void shouldReturnTransactionWhenFound() {
            // Arrange
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            when(transactionRepository.findById(transaction.getId()))
                    .thenReturn(Optional.of(transaction));

            // Act
            Transaction result = orchestrationService
                    .getTransaction(transaction.getId());

            // Assert
            assertThat(result.getId()).isEqualTo(transaction.getId());
        }

        @Test
        @DisplayName("should throw PaymentException when not found")
        void shouldThrowPaymentExceptionWhenNotFound() {
            // Arrange
            UUID randomId = UUID.randomUUID();
            when(transactionRepository.findById(randomId))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() ->
                    orchestrationService.getTransaction(randomId))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("Transaction not found");
        }
    }
}