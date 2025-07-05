package com.aspiresys.fp_micro_orderservice.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Order.
 * Permite operaciones CRUD sobre las órdenes de compra.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    /**
     * Finds all orders associated with a specific user ID.
     * 
     * @param id The ID of the user.
     * @return List of orders associated with the user.
     */
    List<Order> findByUserId(Long id);
}
