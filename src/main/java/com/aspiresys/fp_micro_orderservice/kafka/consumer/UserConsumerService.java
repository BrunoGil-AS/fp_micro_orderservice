package com.aspiresys.fp_micro_orderservice.kafka.consumer;

import com.aspiresys.fp_micro_orderservice.kafka.dto.UserMessage;
import com.aspiresys.fp_micro_orderservice.user.UserSyncService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for receiving user messages from User Service.
 * Handles user synchronization events between User Service and Order Service.
 * 
 * @author bruno.gil
 */
@Service
@Log
public class UserConsumerService {

    @Autowired
    private UserSyncService userSyncService;

    /**
     * Consumes user messages from Kafka topic.
     * 
     * @param userMessage User message payload
     */
    @KafkaListener(
        topics = "${kafka.topic.user:user}",
        containerFactory = "userKafkaListenerContainerFactory"
    )
    public void consumeUserMessage(@Payload UserMessage userMessage) {
        
        try {
            log.info("KAFKA USER: Received message for user: " + userMessage.getEmail());
            log.info("KAFKA USER: Event type: " + userMessage.getEventType());
            
            // Process the user message
            userSyncService.processUserMessage(userMessage);
            
            log.info("KAFKA USER: Successfully processed message for user: " + userMessage.getEmail());
            
        } catch (Exception e) {
            log.warning("KAFKA USER: Error processing message for user " + 
                       (userMessage != null ? userMessage.getEmail() : "unknown") + ": " + e.getMessage());
            e.printStackTrace();
            
            // Re-throw exception to trigger Kafka retry mechanism
            throw new RuntimeException("Failed to process user message", e);
        }
    }
}
