package com.aspiresys.fp_micro_orderservice.order;

import java.time.LocalDateTime;
import java.util.List;

import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import com.aspiresys.fp_micro_orderservice.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Order {
    @Id
    private long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items;

    private LocalDateTime createdAt;
    
    /**
     * Calculates the subtotal for the order.
     * This is the sum of the subtotals of all items in the order.
     * @return the total price of the order, or 0.0 if there are no items.
     */
    public double getTotal() {
        if (items == null) return 0.0;
        return items.stream()
                .mapToDouble(Item::getSubtotal)
                .sum();
    }

    /**
     * Adds a new item to the order.
     * If the item already exists in the order (same product ID), it updates the quantity
     * 
     * @param newItem the item to be added to the order
     */
    public void addItem(Item newItem) {
        if (newItem == null || newItem.getQuantity() <= 0) return;
        if (items == null) return;
        for (Item item : items) {
            if (item.getProduct().getId().equals(newItem.getProduct().getId())) {
                item.setQuantity(item.getQuantity() + newItem.getQuantity());
                return;
            }
        }
        newItem.setOrder(this);
        items.add(newItem);
    }

    /**
     * Removes an item from the order by product ID.
     * If the item does not exist, no action is taken.
     * 
     * @param productId the ID of the product to be removed from the order
     */
    public void removeItemByProductId(Long productId) {
        if (items == null || productId == null) return;
        items.removeIf(item -> item.getProduct().getId().equals(productId));
    }
}
