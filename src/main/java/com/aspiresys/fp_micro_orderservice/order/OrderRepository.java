package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Order.
 * Permite operaciones CRUD sobre las órdenes de compra.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Puedes agregar métodos personalizados aquí si lo necesitas
}
