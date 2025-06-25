package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.web.bind.annotation.*;

import com.aspiresys.fp_micro_orderservice.common.dto.AppResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import java.util.List;

/**
 * Controlador REST para la gestión de órdenes de compra.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    

    @GetMapping
    public ResponseEntity<AppResponse<List<Order>>> getAllOrders() {
        return ResponseEntity.ok(
                new AppResponse<>("Orders retrieved:", orderService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppResponse<Order>> getOrderById(@PathVariable Long id) {
        Order order = orderService.findById(id)
                .orElse(null);
        if (order != null) {
            return ResponseEntity.ok(new AppResponse<>("Order found", order));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppResponse<>("Order not found", null));
        }
    }

    @PostMapping
    public ResponseEntity<AppResponse<Order>> createOrder(@RequestBody Order order) {
        boolean saved = orderService.save(order);
        if (saved) {
            return ResponseEntity.ok(new AppResponse<>("Order created successfully", order));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppResponse<>("Failed to create order", null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AppResponse<Boolean>> deleteOrder(@PathVariable Long id) {
        if (orderService.deleteById(id)) {
            return ResponseEntity.ok(new AppResponse<>("Order deleted successfully", true));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppResponse<>("Order not found", false));
        }
        
    }
}
