package com.aspiresys.fp_micro_orderservice.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * Data Transfer Object for Order entity.
 * <p>
 * This DTO is used to transfer order data between layers without the complexity
 * of JPA entity relationships, avoiding circular references and excessive nesting.
 * </p>
 *
 * @author bruno.gil
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderDTO {
    private Long id;
    private String userEmail;
    private List<ItemDTO> items;
    private LocalDateTime createdAt;
    private double total;
}
