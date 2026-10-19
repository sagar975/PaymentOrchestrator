package com.payments.orchestrator.config;

import com.payments.orchestrator.adapter.messaging.TransactionFailedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration — topics, producer and consumer factories.
 * <p>
 * Explicit producer factory ensures correct JSON serialization
 * of TransactionFailedEvent without type headers.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Topic for failed transaction retry events.
     * 3 partitions for parallel processing.
     * 1 replica for development.
     */
    @Bean
    public NewTopic transactionFailedTopic() {
        return TopicBuilder.name("payment.transaction.failed").partitions(3).replicas(1).build();
    }

    /**
     * Producer factory configured for TransactionFailedEvent.
     * Uses JsonSerializer for value serialization.
     */
    @Bean
    public ProducerFactory<String, TransactionFailedEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * KafkaTemplate for publishing TransactionFailedEvent messages.
     */
    @Bean
    public KafkaTemplate<String, TransactionFailedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}