package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.extern.java.Log;

// AOP imports
import com.aspiresys.fp_micro_orderservice.aop.annotation.Auditable;
import com.aspiresys.fp_micro_orderservice.aop.annotation.ExecutionTime;
import com.aspiresys.fp_micro_orderservice.aop.annotation.ValidateParameters;

/**
 * Implementation of OrderService for managing orders.
 * 
 * <p>
 * Provides methods to find, save, update, and delete orders. This service interacts with the OrderRepository to perform CRUD operations
 * on order entities.
 * </p>
 * @author bruno.gil
 */
@Service
@Log
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @ExecutionTime(operation = "Find All Orders", warningThreshold = 1000)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    @ValidateParameters(notNull = true, message = "Order ID cannot be null")
    @ExecutionTime(operation = "Find Order by ID")
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    /**
     * Saves an order to the repository.
     * 
     * <p>
     * This method checks if the order is null or if it already exists before saving it. It logs the operation and returns true if the
     * order was saved successfully, false otherwise.
     * </p>
     * 
     * @param order The order to save
     * @return True if the order was saved successfully, false otherwise
     */
    @Override
    @Transactional
    @Auditable(operation = "SAVE_ORDER", entityType = "Order", logParameters = true, logResult = true)
    @ExecutionTime(operation = "Save Order", warningThreshold = 2000, detailed = true)
    @ValidateParameters(notNull = true, message = "Order cannot be null")
    public boolean save(Order order) {
        if (order == null) {
            log.warning("Attempted to save null order");
            throw new IllegalArgumentException("Order cannot be null");
        }
        
        log.info("Attempting to save order for user: " + (order.getUser() != null ? order.getUser().getEmail() : "null"));
        log.info("Order items count: " + (order.getItems() != null ? order.getItems().size() : 0));
        
        
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

    /**
     * Deletes an order by its ID.
     * 
     * <p>
     * This method checks if the order ID is null or if the order does not exist before attempting to delete it. It logs the operation and
     * returns true if the order was deleted successfully, false otherwise.
     * </p>
     * 
     * @param id The ID of the order to delete
     * @return True if the order was deleted successfully, false otherwise
     */
    @Override
    @Auditable(operation = "DELETE_ORDER", entityType = "Order", logParameters = true, logResult = true)
    @ExecutionTime(operation = "Delete Order by ID", warningThreshold = 1500)
    @ValidateParameters(notNull = true, message = "Order ID cannot be null")
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

    /**
     * Updates an existing order.
     * 
     * <p>
     * This method checks if the order is null or if it does not exist before attempting to update it. It logs the operation and returns true
     * if the order was updated successfully, false otherwise.
     * </p>
     * 
     * @param order The order to update
     * @return True if the order was updated successfully, false otherwise
     */
    @Override
    @Transactional
    @Auditable(operation = "UPDATE_ORDER", entityType = "Order", logParameters = true, logResult = true)
    @ExecutionTime(operation = "Update Order", warningThreshold = 2000, detailed = true)
    @ValidateParameters(notNull = true, message = "Order cannot be null for update")
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

    /**
     * Finds orders by user ID.
     * 
     * <p>
     * This method checks if the user ID is null before attempting to find orders. It logs the operation and returns a list of orders associated
     * with the user ID.
     * </p>
     * 
     * @param id The ID of the user
     * @return List of orders associated with the user
     */
    @Override
    @ExecutionTime(operation = "Find Orders by User ID")
    @ValidateParameters(notNull = true, message = "User ID cannot be null")
    public List<Order> findByUserId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return orderRepository.findByUserId(id);
    }
}
