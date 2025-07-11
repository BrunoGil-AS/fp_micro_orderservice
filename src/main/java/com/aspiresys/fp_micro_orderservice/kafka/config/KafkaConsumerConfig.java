package com.aspiresys.fp_micro_orderservice.kafka.config;

import com.aspiresys.fp_micro_orderservice.kafka.dto.ProductMessage;
import lombok.extern.java.Log;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for Order Service consumer.
 * Configures the consumer to receive product messages from the product service.
 * 
 * @author bruno.gil
 */
@Configuration
@EnableKafka
@Log
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:product-group}")
    private String groupId;

    /**
     * Consumer factory configuration for ProductMessage consumption.
     * 
     * @return ConsumerFactory for ProductMessage
     */
    @Bean
    public ConsumerFactory<String, ProductMessage> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ProductMessage.class.getName());
        
        // Additional resilience configurations
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        log.info("📋 KAFKA CONSUMER CONFIG: Bootstrap servers: " + bootstrapServers);
        log.info("📋 KAFKA CONSUMER CONFIG: Group ID: " + groupId);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka listener container factory for ProductMessage consumption.
     * 
     * @return ConcurrentKafkaListenerContainerFactory for ProductMessage
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ProductMessage> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Configure error handling with limited retries
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            new FixedBackOff(1000L, 3) // 3 retries with 1 second interval
        );
        
        // Custom error handling for missing headers
        errorHandler.addNotRetryableExceptions(
            org.springframework.messaging.MessageHandlingException.class
        );
        
        factory.setCommonErrorHandler(errorHandler);
        factory.setConcurrency(1); // Single thread to avoid conflicts
        factory.setAutoStartup(true);
        
        log.info("🔧 KAFKA LISTENER FACTORY: Configured with error handling and concurrency=1");
        
        return factory;
    }
}
