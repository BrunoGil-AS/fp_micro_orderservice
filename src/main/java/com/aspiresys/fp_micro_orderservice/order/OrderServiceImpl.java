package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.extern.java.Log;

/**
 * Implementación de OrderService para la gestión de órdenes de compra.
 */
@Service
@Log
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
    @Transactional
    public boolean save(Order order) {
        if (order == null) {
            log.warning("Attempted to save null order");
            throw new IllegalArgumentException("Order cannot be null");
        }
        
        log.info("Attempting to save order for user: " + (order.getUser() != null ? order.getUser().getEmail() : "null"));
        log.info("Order items count: " + (order.getItems() != null ? order.getItems().size() : 0));
        
        // Solo verifica existencia si el id NO es null
        if (order.getId() != null && orderRepository.existsById(order.getId())) {
            log.warning("Order with ID " + order.getId() + " already exists");
            throw new IllegalArgumentException("Order with ID " + order.getId() + " already exists");
        }
        
        try {
            Order savedOrder = orderRepository.save(order);
            boolean success = savedOrder != null;
            if (success) {
                log.info("Order saved successfully with ID: " + savedOrder.getId());
            } else {
                log.warning("Failed to save order - repository returned null");
            }
            return success;
        } catch (Exception e) {
            log.severe("Error saving order: " + e.getMessage());
            throw e;
        }
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
    @Transactional
    public boolean update(Order order) {
        if (order == null) {
            log.warning("Attempted to update null order");
            throw new IllegalArgumentException("Order cannot be null");
        }
        
        if (order.getId() == null) {
            log.warning("Attempted to update order without ID");
            throw new IllegalArgumentException("Order ID cannot be null for update");
        }
        
        if (!orderRepository.existsById(order.getId())) {
            log.warning("Order with ID " + order.getId() + " does not exist for update");
            return false;
        }
        
        try {
            Order updatedOrder = orderRepository.save(order);
            boolean success = updatedOrder != null;
            if (success) {
                log.info("Order updated successfully with ID: " + updatedOrder.getId());
            } else {
                log.warning("Failed to update order - repository returned null");
            }
            return success;
        } catch (Exception e) {
            log.severe("Error updating order: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Order> findByUserId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return orderRepository.findByUserId(id);
    }
}
