package com.aspiresys.fp_micro_orderservice.order.Item;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Finds all items associated with a specific order ID.
     * JPA translates this method into a SQL query that retrieves all items
     * where the order ID matches the provided parameter.
     * 
     * @param orderId
     * @return a list of items associated with the specified order ID
     *         or an empty list if no items are found.
     */
    List<Item> findByOrderId(Long orderId);

    /**
     * Finds all items associated with a specific product ID.
     * JPA translates this method into a SQL query that retrieves all items
     * where the product ID matches the provided parameter.
     * 
     * @param productId
     * @return a list of items associated with the specified product ID
     *         or an empty list if no items are found.
     */
    List<Item> findByProductId(Long productId);
}
