package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.aspiresys.fp_micro_orderservice.common.dto.AppResponse;
import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import com.aspiresys.fp_micro_orderservice.order.dto.CreateOrderDTO;
import com.aspiresys.fp_micro_orderservice.order.dto.OrderDTO;
import com.aspiresys.fp_micro_orderservice.order.dto.OrderMapper;
import com.aspiresys.fp_micro_orderservice.user.User;
import com.aspiresys.fp_micro_orderservice.user.UserService;

import lombok.extern.java.Log;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 
 */
/**
 * REST controller for managing orders.
 * <p>
 * Provides endpoints to create, retrieve, and delete orders.
 * </p>
 *
 * <ul>
 *   <li>{@code GET /orders} - Retrieves all orders.</li>
 *   <li>{@code GET /orders/{id}} - Retrieves a specific order by its ID.</li>
 *   <li>{@code POST /orders} - Creates a new order.</li>
 *   <li>{@code DELETE /orders/{id}} - Deletes an order by its ID.</li>
 * </ul>
 *
 * Responses are wrapped in {@link AppResponse} objects for consistent API structure.
 *
 * @author bruno.gil
 */
@RestController
@RequestMapping("/orders")
@Log
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderValidationService orderValidationService;
    @Autowired
    private OrderMapper orderMapper;

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')") // Only ADMIN can access this endpoint
    public ResponseEntity<AppResponse<List<OrderDTO>>> getAllOrders() {
        List<Order> orders = orderService.findAll();
        List<OrderDTO> orderDTOs = orderMapper.toDTOList(orders);
        return ResponseEntity.ok(
                new AppResponse<>("Orders retrieved:", orderDTOs));
    }

    @GetMapping("/find")
    @PreAuthorize("hasRole('ADMIN')") // Allow both ADMIN and USER to access
    public ResponseEntity<AppResponse<OrderDTO>> getOrderById(@RequestParam Long id) {
        Order order = orderService.findById(id)
                .orElse(null);
        if (order != null) {
            OrderDTO orderDTO = orderMapper.toDTO(order);
            return ResponseEntity.ok(new AppResponse<>("Order found", orderDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppResponse<>("Order not found", null));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')") // Solo usuarios con rol USER pueden acceder a sus propias órdenes
    public ResponseEntity<AppResponse<List<OrderDTO>>> getOrdersByUser(Authentication authentication) {
        String email = ((Jwt) (authentication.getPrincipal())).getClaimAsString("sub");
        User user = userService.getUserByEmail(email);
        
        if (user == null) {
            log.warning("User with email " + email + " does not exist.");
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new AppResponse<>("User does not have any orders", new ArrayList<>()));
        }
        List<Order> orders = orderService.findByUserId(user.getId());
        List<OrderDTO> orderDTOs = orderMapper.toDTOList(orders);
        log.info("Retrieved " + orders.size() + " orders for user: " + email);
        return ResponseEntity.ok(new AppResponse<>("Orders retrieved for user: " + email, orderDTOs));
    }


    @PostMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AppResponse<OrderDTO>> createOrder(@RequestBody CreateOrderDTO createOrderDTO, Authentication authentication) {
        String email = ((Jwt) (authentication.getPrincipal())).getClaimAsString("sub");
        try {
            // Convert DTO to Order entity
            User user = userService.getUserByEmail(email);
            if (user == null) {
                log.warning("User with email " + email + " does not exist.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AppResponse<>("User does not exist", null));
            }
            
            Order order = orderMapper.toEntity(createOrderDTO, user);
            
            // Validate order asynchronously (user and products in parallel)
            OrderValidationService.OrderValidationResult validationResult = 
                orderValidationService.validateOrderAsync(order).get();
            
            if (!validationResult.isValid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppResponse<>(validationResult.getErrorMessage(), null));
            }
            
            User validatedUser = validationResult.getUser();
            if (validatedUser == null) {
                log.warning("User with email " + email + " does not exist.");
                if (validationResult.getErrorMessage() != null) {
                    log.severe("Validation error: " + validationResult.getErrorMessage());
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AppResponse<>("User does not exist", null));
            }
            
            if (validatedUser.getEmail() == null || !validatedUser.getEmail().equals(email)) {
                log.warning("User email mismatch: expected " + email + ", got " + validatedUser.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AppResponse<>("You are not allowed to create orders for others", null));
            }

            // Ensure the user is saved or updated in the user service
            userService.saveUser(validatedUser);
            
            // Create new order with validated user
            Order newOrder = Order.builder()
                        .user(validatedUser)
                        .createdAt(LocalDateTime.now())
                        .build();
            
            // Set up items with validated products
            for (Item item : order.getItems()) {
                item.setOrder(newOrder);
            }
            
            newOrder.setItems(new ArrayList<>(order.getItems())); // Set items from the request
            
            // Save the order
            if (orderService.save(newOrder)) {
                OrderDTO orderDTO = orderMapper.toDTO(newOrder);
                return ResponseEntity.ok(new AppResponse<>("Order created successfully", orderDTO));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new AppResponse<>("Failed to create order", null));
            }
        } catch (Exception e) {
            log.warning("Error creating order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppResponse<>("Error creating order: " + e.getMessage(), null));
        }
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AppResponse<OrderDTO>> updateOrder(@RequestBody CreateOrderDTO updateOrderDTO, @RequestParam Long orderId, Authentication authentication) {
        String email = ((Jwt) (authentication.getPrincipal())).getClaimAsString("sub");
        User user = userService.getUserByEmail(email);
        if (user == null) {
            log.warning("User " + email + " attempted to update order but does not exist.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppResponse<>("User not found", null));
        }
        
        Order existingOrder = orderService.findById(orderId)
                .orElse(null);
        if (existingOrder == null) {
            log.warning("User " + user.getEmail() + " attempted to update order " + orderId + " that does not exist.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppResponse<>("Order not found", null));
        }
        if (!existingOrder.getUser().getId().equals(user.getId())) {
            log.warning("User " + user.getEmail() + " attempted to update order " + orderId + " that does not belong to them.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)// For security reasons, this service won't tell if the order exists or not
                    .body(new AppResponse<>("Order not found", null));
        }
        
        // Convert DTO to Order entity items
        Order updateOrder = orderMapper.toEntity(updateOrderDTO, user);
        existingOrder.setItems(updateOrder.getItems());
        
        try {
            if (orderService.update(existingOrder)) {
                OrderDTO orderDTO = orderMapper.toDTO(existingOrder);
                return ResponseEntity.ok(new AppResponse<>("Order updated successfully", orderDTO));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new AppResponse<>("Failed to update order", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppResponse<>("Error updating order: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AppResponse<Boolean>> deleteOrder(@RequestParam Long id, Authentication authentication) {

        String email = ((Jwt) (authentication.getPrincipal())).getClaimAsString("sub");
        User user = userService.getUserByEmail(email);
        Order order = orderService.findById(id)
                .orElse(null);
        if (user == null) {
            log.warning("User " + email + " attempted to delete order " + id + " but does not exist.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppResponse<>("User not found", false)); 
        }
        if (order == null) {
            log.warning("User " + user.getEmail() + " attempted to delete order " + id + " that does not exist.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppResponse<>("Order not found", false));
        }
        if (!order.getUser().getId().equals(user.getId())) {
            log.warning("User " + user.getEmail() + " attempted to delete order " + id + " that does not belong to them.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)// For security reasons, this service won't tell if the order exists or not
                    .body(new AppResponse<>("Order not found", false));
        }
        try {
            orderService.deleteById(id);
            return ResponseEntity.ok(new AppResponse<>("Order deleted successfully", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppResponse<>("Error deleting order: " + e.getMessage(), false));
        }
        
    }
}
