package com.aspiresys.fp_micro_orderservice.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for product messages received via Kafka from product service.
 * Contains product information for communication between microservices.
 * 
 * @author bruno.gil
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMessage {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("price")
    private Double price;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("imageUrl")
    private String imageUrl;
    
    @JsonProperty("stock")
    private Integer stock;
    
    @JsonProperty("brand")
    private String brand; // Optional field, depends on product type
    
    @JsonProperty("eventType")
    private String eventType; // "PRODUCT_CREATED", "PRODUCT_UPDATED", "PRODUCT_DELETED", "INITIAL_LOAD"
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    // Constructor for creating messages from Product entities
    public ProductMessage(Long id, String name, Double price, String category, 
                         String imageUrl, Integer stock, String brand, String eventType) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.stock = stock;
        this.brand = brand;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }
}
