package com.aspiresys.fp_micro_orderservice.order.dto;

import java.util.List;
import lombok.*;

/**
 * Data Transfer Object for creating new orders.
 * <p>
 * This DTO is used when receiving order creation requests from clients,
 * containing only the necessary information to create a new order.
 * </p>
 *
 * @author bruno.gil
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreateOrderDTO {
    private List<CreateItemDTO> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateItemDTO {
        private Long productId;
        private int quantity;
    }
}
