package com.payments.orchestrator.adapter.persistence;
import com.payments.orchestrator.domain.model.TransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a transaction event row in the database.
 * <p>
 * Maps to transaction_events table — stores complete audit trail
 * of every state transition a transaction goes through.
 * Records are never deleted — permanent audit history.
 */
@Entity
@Table(name = "transaction_events")
@Getter
@Setter
@NoArgsConstructor

public class TransactionEventJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private TransactionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private TransactionStatus toStatus;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
