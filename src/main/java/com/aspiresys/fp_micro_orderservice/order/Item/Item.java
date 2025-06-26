package com.aspiresys.fp_micro_orderservice.order.Item;

import com.aspiresys.fp_micro_orderservice.order.Order;
import com.aspiresys.fp_micro_orderservice.product.Product;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents an item in an order, linking a product and its quantity to a specific order.
 * <p>
 * Each Item is associated with an {@link Order} and a {@link Product}.
 * The subtotal for the item is calculated as the product's price multiplied by the quantity.
 * </p>
 *
 * <p>
 * Fields:
 * <ul>
 *   <li>id - Unique identifier for the item.</li>
 *   <li>order - The order to which this item belongs.</li>
 *   <li>product - The product associated with this item.</li>
 *   <li>quantity - The quantity of the product in the order.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Methods:
 * <ul>
 *   <li>{@code getSubtotal()} - Calculates the subtotal price for this item.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class is annotated as a JPA entity and maps to the "items" table in the database.
 * </p>
 * 
 * <p>@author bruno.gil</p>
 * <p>See {@link Order} and {@link Product} for related entities. </p>
 */
@Entity
@Table(name = "items")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    public double getSubtotal() {
        if (product == null || product.getPrice() == null) {
            return 0.0;
        }
        return product.getPrice() * quantity;
    }

}
