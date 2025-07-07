package com.aspiresys.fp_micro_orderservice.order.dto;

import lombok.*;

/**
 * Data Transfer Object for Item entity.
 * <p>
 * This DTO is used to transfer item data without JPA entity relationships,
 * containing only the essential information needed for order processing.
 * </p>
 *
 * @author bruno.gil
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Double productPrice;
    private String productCategory;
    private String productImageUrl;
    private int quantity;
    private double subtotal;
}
