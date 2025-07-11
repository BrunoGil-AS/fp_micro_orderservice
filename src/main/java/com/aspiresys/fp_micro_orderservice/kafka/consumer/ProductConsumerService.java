package com.aspiresys.fp_micro_orderservice.kafka.consumer;

import com.aspiresys.fp_micro_orderservice.kafka.dto.ProductMessage;
import com.aspiresys.fp_micro_orderservice.product.ProductSyncService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for receiving product messages from Product Service.
 * Listens to the product topic and synchronizes product information to the existing products table.
 * 
 * @author bruno.gil
 */
@Service
@Log
public class ProductConsumerService {

    @Autowired
    private ProductSyncService productSyncService;

    /**
     * Consumes product messages from Kafka topic.
     * Processes different types of product events (CREATED, UPDATED, DELETED, INITIAL_LOAD).
     * 
     * @param productMessage The product message from Kafka
     * @param key The message key
     * @param topic The topic name
     * @param partition The partition number
     */
    @KafkaListener(topics = "${spring.kafka.topic.product}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeProductMessage(
            @Payload ProductMessage productMessage,
            @Header(KafkaHeaders.KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        
        try {
            log.info("📨 KAFKA: Received message - Topic: " + topic + 
                     ", Event: " + productMessage.getEventType() + 
                     ", Product: " + productMessage.getName() + " (ID: " + productMessage.getId() + ")");

            // Process message based on event type
            switch (productMessage.getEventType()) {
                case "PRODUCT_CREATED":
                    handleProductCreated(productMessage);
                    break;
                    
                case "PRODUCT_UPDATED":
                    handleProductUpdated(productMessage);
                    break;
                    
                case "PRODUCT_DELETED":
                    handleProductDeleted(productMessage);
                    break;
                    
                case "INITIAL_LOAD":
                    handleInitialLoad(productMessage);
                    break;
                    
                default:
                    log.warning("Unknown event type received: " + productMessage.getEventType());
                    break;
            }
            
        } catch (Exception e) {
            log.severe("Error processing product message: " + e.getMessage());
            e.printStackTrace();
            // In a production environment, you might want to send this to a dead letter queue
        }
    }

    /**
     * Handles product created events
     */
    private void handleProductCreated(ProductMessage productMessage) {
        productSyncService.saveOrUpdateProduct(productMessage);
    }

    /**
     * Handles product updated events
     */
    private void handleProductUpdated(ProductMessage productMessage) {
        productSyncService.saveOrUpdateProduct(productMessage);
    }

    /**
     * Handles product deleted events
     */
    private void handleProductDeleted(ProductMessage productMessage) {
        productSyncService.deleteProduct(productMessage.getId());
    }

    /**
     * Handles initial load events (when product service starts up)
     */
    private void handleInitialLoad(ProductMessage productMessage) {
        productSyncService.saveOrUpdateProduct(productMessage);
    }
}
