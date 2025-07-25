package com.aspiresys.fp_micro_orderservice.kafka.producer;

import com.aspiresys.fp_micro_orderservice.kafka.dto.ProductMessage;
import com.aspiresys.fp_micro_orderservice.product.Product;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Producer service for sending product messages to Kafka topics.
 * Handles product events and communicates with other microservices.
 * 
 * @author bruno.gil
 */
@Service
@Log
public class ProductProducerService {

    @Autowired
    private KafkaTemplate<String, ProductMessage> kafkaTemplate;

    @Value("${kafka.topic.product:product}")
    private String productTopic;

    /**
     * Sends initial product list to Kafka topic.
     * This is called on application startup or when requested.
     * 
     * @param products List of all products to send
     */
    public void sendInitialProductList(List<Product> products) {
        log.info("Sending initial product list to Kafka. Total products: " + products.size());
        
        for (Product product : products) {
            ProductMessage message = createProductMessage(product, "INITIAL_LOAD");
            sendProductMessage(message);
        }
        
        log.info("Initial product list sent successfully to topic: " + productTopic);
    }


    /**
     * Sends a product updated event to Kafka topic.
     * 
     * @param product The updated product
     */
    public void sendProductUpdated(Product product) {
        log.info("Sending product updated event for product ID: " + product.getId());
        ProductMessage message = createProductMessage(product, "PRODUCT_UPDATED");
        sendProductMessage(message);
    }

    

    /**
     * Creates a ProductMessage from a Product entity.
     * 
     * @param product The product entity
     * @param eventType The type of event
     * @return ProductMessage for Kafka
     */
    private ProductMessage createProductMessage(Product product, String eventType) {
        
        
        return new ProductMessage(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getCategory(),
            product.getImageUrl(),
            product.getStock(),
            product.getBrand(),
            eventType
        );
    }

    /**
     * Sends a product message to the Kafka topic.
     * 
     * @param message The product message to send
     */
    private void sendProductMessage(ProductMessage message) {
        try {
            CompletableFuture<SendResult<String, ProductMessage>> future = 
                kafkaTemplate.send(productTopic, message.getId().toString(), message);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("Product message sent successfully. Topic: " + productTopic + 
                            ", Key: " + message.getId() + ", Event: " + message.getEventType());
                } else {
                    log.severe("Failed to send product message. Topic: " + productTopic + 
                              ", Key: " + message.getId() + ", Error: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            log.severe("Error sending product message to Kafka: " + e.getMessage());
            throw new RuntimeException("Failed to send product message", e);
        }
    }
}
