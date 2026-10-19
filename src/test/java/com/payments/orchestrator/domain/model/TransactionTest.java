package com.payments.orchestrator.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Transaction domain model.
 * Tests state machine transitions and business rules.
 * No Spring context — pure Java tests.
 */
@DisplayName("Transaction Domain Model")
class TransactionTest {

    private static final String MERCHANT_ID = "merchant-001";
    private static final BigDecimal AMOUNT = new BigDecimal("999.99");
    private static final String CURRENCY = "INR";
    private static final String IDEMPOTENCY_KEY = "order-123";

    @Nested
    @DisplayName("Transaction Creation")
    class Creation {

        @Test
        @DisplayName("should create transaction with INITIATED status")
        void shouldCreateTransactionWithInitiatedStatus() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            assertThat(transaction.getId()).isNotNull();
            assertThat(transaction.getMerchantId()).isEqualTo(MERCHANT_ID);
            assertThat(transaction.getAmount()).isEqualTo(AMOUNT);
            assertThat(transaction.getCurrency()).isEqualTo("INR");
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.INITIATED);
            assertThat(transaction.getRetryCount()).isZero();
            assertThat(transaction.getCreatedAt()).isNotNull();
            assertThat(transaction.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should convert currency to uppercase")
        void shouldConvertCurrencyToUppercase() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, "inr", IDEMPOTENCY_KEY);

            assertThat(transaction.getCurrency()).isEqualTo("INR");
        }

        @Test
        @DisplayName("should throw exception when merchantId is blank")
        void shouldThrowExceptionWhenMerchantIdIsBlank() {
            assertThatThrownBy(() ->
                    Transaction.create("", AMOUNT, CURRENCY, IDEMPOTENCY_KEY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("merchantId");
        }

        @Test
        @DisplayName("should throw exception when amount is zero")
        void shouldThrowExceptionWhenAmountIsZero() {
            assertThatThrownBy(() ->
                    Transaction.create(MERCHANT_ID, BigDecimal.ZERO,
                            CURRENCY, IDEMPOTENCY_KEY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw exception when amount is negative")
        void shouldThrowExceptionWhenAmountIsNegative() {
            assertThatThrownBy(() ->
                    Transaction.create(MERCHANT_ID,
                            new BigDecimal("-1.00"),
                            CURRENCY, IDEMPOTENCY_KEY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw exception when idempotencyKey is blank")
        void shouldThrowExceptionWhenIdempotencyKeyIsBlank() {
            assertThatThrownBy(() ->
                    Transaction.create(MERCHANT_ID, AMOUNT, CURRENCY, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idempotencyKey");
        }
    }

    @Nested
    @DisplayName("State Machine Transitions")
    class StateMachine {

        @Test
        @DisplayName("should transition from INITIATED to ROUTING")
        void shouldTransitionFromInitiatedToRouting() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            transaction.transitionTo(TransactionStatus.ROUTING);

            assertThat(transaction.getStatus())
                    .isEqualTo(TransactionStatus.ROUTING);
        }

        @Test
        @DisplayName("should transition through full happy path")
        void shouldTransitionThroughFullHappyPath() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            transaction.transitionTo(TransactionStatus.ROUTING);
            transaction.transitionTo(TransactionStatus.AUTHORIZED);
            transaction.transitionTo(TransactionStatus.CAPTURED);
            transaction.transitionTo(TransactionStatus.SETTLED);

            assertThat(transaction.getStatus())
                    .isEqualTo(TransactionStatus.SETTLED);
        }

        @Test
        @DisplayName("should throw exception on invalid transition")
        void shouldThrowExceptionOnInvalidTransition() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            // Cannot go from INITIATED directly to AUTHORIZED
            assertThatThrownBy(() ->
                    transaction.transitionTo(TransactionStatus.AUTHORIZED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid transition");
        }

        @Test
        @DisplayName("should throw exception when transitioning from terminal state")
        void shouldThrowExceptionWhenTransitioningFromTerminalState() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            transaction.transitionTo(TransactionStatus.ROUTING);
            transaction.transitionTo(TransactionStatus.AUTHORIZED);
            transaction.transitionTo(TransactionStatus.CAPTURED);
            transaction.transitionTo(TransactionStatus.SETTLED);

            // SETTLED is terminal — cannot transition further
            assertThatThrownBy(() ->
                    transaction.transitionTo(TransactionStatus.ROUTING))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("terminal");
        }

        @Test
        @DisplayName("should increment retry count on markFailed")
        void shouldIncrementRetryCountOnMarkFailed() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            transaction.transitionTo(TransactionStatus.ROUTING);
            transaction.markFailed();

            assertThat(transaction.getRetryCount()).isEqualTo(1);
            assertThat(transaction.getStatus())
                    .isEqualTo(TransactionStatus.FAILED);
        }

        @Test
        @DisplayName("should allow retry when retryCount below maximum")
        void shouldAllowRetryWhenRetryCountBelowMaximum() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            transaction.transitionTo(TransactionStatus.ROUTING);
            transaction.markFailed();

            assertThat(transaction.canRetry()).isTrue();
        }
    }

    @Nested
    @DisplayName("Gateway Assignment")
    class GatewayAssignment {

        @Test
        @DisplayName("should assign gateway name")
        void shouldAssignGatewayName() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            transaction.assignGateway("razorpay");

            assertThat(transaction.getGatewayUsed()).isEqualTo("razorpay");
        }

        @Test
        @DisplayName("should throw exception when gateway name is blank")
        void shouldThrowExceptionWhenGatewayNameIsBlank() {
            Transaction transaction = Transaction.create(
                    MERCHANT_ID, AMOUNT, CURRENCY, IDEMPOTENCY_KEY);

            assertThatThrownBy(() -> transaction.assignGateway(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("gatewayName");
        }
    }
}