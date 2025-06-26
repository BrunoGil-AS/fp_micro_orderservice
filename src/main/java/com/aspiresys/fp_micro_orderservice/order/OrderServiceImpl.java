package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de OrderService para la gestión de órdenes de compra.
 */
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public boolean save(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        // Solo verifica existencia si el id NO es null
        if (order.getId() != null && orderRepository.existsById(order.getId())) {
            throw new IllegalArgumentException("Order with ID " + order.getId() + " already exists");
        }
        return orderRepository.save(order) != null;
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (!orderRepository.existsById(id)) {
            return false; // Order not found
        }
        orderRepository.deleteById(id);
        return !orderRepository.existsById(id); // Check if deletion was successful
    }

    @Override
    public boolean update(Order order) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }
}
