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
    private ProductSyncService productSyncService;    /**
     * Consumes product messages from Kafka topic.
     * Processes different types of product events (CREATED, UPDATED, DELETED, INITIAL_LOAD).
     * 
     * @param productMessage The product message from Kafka
     * @param key The message key (optional)
     * @param topic The topic name (optional)
     * @param partition The partition number (optional)
     */
    @KafkaListener(topics = "${kafka.topic.product:product}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeProductMessage(
            @Payload ProductMessage productMessage,
            @Header(value = KafkaHeaders.KEY, required = false) String key,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition) {

        try {
            log.info("📨 KAFKA: Received message from topic: " + (topic != null ? topic : "unknown") + 
                    ", partition: " + (partition != null ? partition : "unknown") + 
                    ", key: " + (key != null ? key : "no-key"));
            log.info("📨 KAFKA: Message details - Event: " + productMessage.getEventType() + 
                    ", Product: " + productMessage.getName() + 
                    " (ID: " + productMessage.getId() + ")" +
                    ", Brand: " + productMessage.getBrand() +
                    ", Stock: " + productMessage.getStock());

            // Process message based on event type
            processProductMessage(productMessage);
            
            log.info("✅ KAFKA: Successfully processed message for product ID: " + productMessage.getId());
            
        } catch (Exception e) {
            log.severe("❌ KAFKA ERROR: Failed to process product message: " + e.getMessage());
            log.severe("❌ Message details - ID: " + productMessage.getId() + 
                      ", Event: " + productMessage.getEventType() + 
                      ", Name: " + productMessage.getName());
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

    /**
     * Alternative consumer method without headers for compatibility.
     * This method can be uncommented if header issues persist.
     * IMPORTANT: If you enable this method, comment out the main consumeProductMessage method above
     * to avoid duplicate consumption.
     * 
     * @param productMessage The product message from Kafka
     */
    /*
    @KafkaListener(topics = "${kafka.topic.product:product}", groupId = "${spring.kafka.consumer.group-id}-simple")
    public void consumeProductMessageSimple(@Payload ProductMessage productMessage) {
        try {
            log.info("📨 KAFKA SIMPLE: Received message - Event: " + productMessage.getEventType() + 
                    ", Product: " + productMessage.getName() + 
                    " (ID: " + productMessage.getId() + ")");

            // Process message based on event type
            processProductMessage(productMessage);
            
            log.info("✅ KAFKA SIMPLE: Successfully processed message for product ID: " + productMessage.getId());
            
        } catch (Exception e) {
            log.severe("❌ KAFKA SIMPLE ERROR: Failed to process product message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    */

    /**
     * Common method to process product messages regardless of consumer method used.
     * 
     * @param productMessage The product message to process
     */
    private void processProductMessage(ProductMessage productMessage) {
        switch (productMessage.getEventType()) {
            case "PRODUCT_CREATED":
                log.info("🆕 Processing PRODUCT_CREATED event for ID: " + productMessage.getId());
                handleProductCreated(productMessage);
                break;
                
            case "PRODUCT_UPDATED":
                log.info("📝 Processing PRODUCT_UPDATED event for ID: " + productMessage.getId());
                handleProductUpdated(productMessage);
                break;
                
            case "PRODUCT_DELETED":
                log.info("🗑️ Processing PRODUCT_DELETED event for ID: " + productMessage.getId());
                handleProductDeleted(productMessage);
                break;
                
            case "INITIAL_LOAD":
                log.info("📦 Processing INITIAL_LOAD event for ID: " + productMessage.getId());
                handleInitialLoad(productMessage);
                break;
                
            default:
                log.warning("❓ Unknown event type received: " + productMessage.getEventType());
                break;
        }
    }
}
