package com.aspiresys.fp_micro_orderservice.order;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz para el servicio de gestión de órdenes de compra.
 */
public interface OrderService {
    /**
     * Finds all orders.
     * @return List of all orders.
     */
    List<Order> findAll();

    /**
     * Finds an order by its ID.
     * @param id The ID of the order.
     * @return An Optional containing the order if found, or empty if not found.
     */
    Optional<Order> findById(Long orderId);

    /**
     * Saves an order.
     * @param order The order to save.
     * @return True if the order was saved successfully, false otherwise.
     */
    boolean save(Order order);

    /**
     * Deletes an order by its ID.
     * @param id The ID of the order to delete.
     * @return True if the order was deleted successfully, false otherwise.
     */
    boolean deleteById(Long orderId);

    /**
     * Updates an existing order.
     * @param order The order to update.
     * @return True if the order was updated successfully, false otherwise.
     * @throws IllegalArgumentException if the order is null or does not exist.
     */
    boolean update(Order order);

    /**
     * Finds orders by user ID.
     * @param id The ID of the user.
     * @return List of orders associated with the user.
     */
    List<Order> findByUserId(Long id);
}
