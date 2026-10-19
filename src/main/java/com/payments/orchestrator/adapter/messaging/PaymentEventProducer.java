package com.payments.orchestrator.adapter.messaging;

import com.payments.orchestrator.domain.model.Transaction;
import com.payments.orchestrator.domain.port.PaymentEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes payment events to Kafka topics.
 * <p>
 * Implements PaymentEventPort — keeps domain layer
 * independent of Kafka implementation details.
 * <p>
 * Decouples gateway failure handling from the main
 * transaction flow — retry happens asynchronously.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer implements PaymentEventPort {

    private static final String TOPIC_TRANSACTION_FAILED =
            "payment.transaction.failed";

    private final KafkaTemplate<String, TransactionFailedEvent> kafkaTemplate;

    /**
     * Publishes a transaction failed event to Kafka.
     * Consumer will pick this up and retry on fallback gateway.
     *
     * @param transaction       the failed transaction
     * @param failedGatewayName name of the gateway that failed
     */
    @Override
    public void publishTransactionFailed(
            Transaction transaction,
            String failedGatewayName) {

        TransactionFailedEvent event = new TransactionFailedEvent(
                transaction.getId(),
                failedGatewayName,
                transaction.getMerchantId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getIdempotencyKey(),
                transaction.getRetryCount());

        log.info("Publishing transaction failed event. txnId={}, gateway={}",
                transaction.getId(), failedGatewayName);

        kafkaTemplate.send(
                TOPIC_TRANSACTION_FAILED,
                transaction.getId().toString(),
                event);

        log.info("Transaction failed event published. txnId={}",
                transaction.getId());
    }
}