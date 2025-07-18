package com.aspiresys.fp_micro_orderservice.kafka.config;

import com.aspiresys.fp_micro_orderservice.kafka.dto.ProductMessage;
import com.aspiresys.fp_micro_orderservice.kafka.dto.UserMessage;
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

    @Value("${spring.kafka.consumer.user-group-id:user-group}")
    private String userGroupId;

    /**
     * Consumer factory configuration for ProductMessage consumption.
     * 
     * @return ConsumerFactory for ProductMessage
     */
    @Bean
    public ConsumerFactory<String, ProductMessage> productConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class); 
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");// Start from the earliest message
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ProductMessage.class.getName());// Deserialize ProductMessage objects
        
        // Additional resilience configurations
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);// Enable auto-commit for offsets
        // This allows the consumer to automatically commit offsets after processing messages
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);// Limit the number of records returned in a single poll
        // This helps manage memory usage and processing time
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        // This sets the maximum time between polls before the consumer is considered dead
        
        log.info("KAFKA Product CONSUMER CONFIG: Bootstrap servers: " + bootstrapServers);
        log.info("KAFKA Product CONSUMER CONFIG: Group ID: " + groupId);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka listener container factory for ProductMessage consumption.
     * 
     * @return ConcurrentKafkaListenerContainerFactory for ProductMessage
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductMessage> productKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ProductMessage> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(productConsumerFactory());
        
        // Configure error handling with limited retries
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            new FixedBackOff(1000L, 3) // 3 retries with 1 second interval
        );
        
        // Custom error handling for missing headers
        errorHandler.addNotRetryableExceptions(
            org.springframework.messaging.MessageHandlingException.class
        );
        
        factory.setCommonErrorHandler(errorHandler);
        factory.setConcurrency(1); // Single thread to avoid conflicts of processing
        // This ensures that messages are processed in order and avoids concurrency issues
        factory.setAutoStartup(true);
        
        log.info("KAFKA Product LISTENER FACTORY: Configured with error handling and concurrency=1");
        
        return factory;
    }

    /**
     * Consumer factory configuration for UserMessage consumption.
     * 
     * @return ConsumerFactory for UserMessage
     */
    @Bean
    public ConsumerFactory<String, UserMessage> userConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, userGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, UserMessage.class.getName());
        
        // Additional resilience configurations
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        log.info("KAFKA User CONSUMER CONFIG: Bootstrap servers: " + bootstrapServers);
        log.info("KAFKA User CONSUMER CONFIG: Group ID: " + userGroupId);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka listener container factory for UserMessage consumption.
     * 
     * @return ConcurrentKafkaListenerContainerFactory for UserMessage
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserMessage> userKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserMessage> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userConsumerFactory());
        
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
        
        log.info("KAFKA USER LISTENER FACTORY: Configured with error handling and concurrency=1");
        
        return factory;
    }
}
