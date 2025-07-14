package com.aspiresys.fp_micro_orderservice.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for User messages received via Kafka.
 * Contains user information for communication between microservices.
 * 
 * @author bruno.gil
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserMessage {

    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("eventType")
    private String eventType; // "USER_CREATED", "USER_UPDATED", "USER_DELETED", "INITIAL_LOAD"
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
