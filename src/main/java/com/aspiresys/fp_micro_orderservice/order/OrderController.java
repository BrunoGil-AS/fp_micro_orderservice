package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.web.bind.annotation.*;

import com.aspiresys.fp_micro_orderservice.common.dto.AppResponse;
import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import com.aspiresys.fp_micro_orderservice.product.Product;
import com.aspiresys.fp_micro_orderservice.product.ProductService;
import com.aspiresys.fp_micro_orderservice.user.User;
import com.aspiresys.fp_micro_orderservice.user.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;


    @Autowired
    private WebClient.Builder webClientBuilder;

    

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
        // Validar existencia de usuario y producto a través del gateway
        String userUrl = "http://localhost:8080/user-service/users/find?email=" + order.getUser().getEmail();
        String productUrl = "http://localhost:8080/product-service/products";
        User user = null;
        List<Product> products = null;

        // Validar y obtener usuario
        try {
            AppResponse<User> userResponse = webClientBuilder.build()
                .get()
                .uri(userUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<AppResponse<User>>() {})
                .block();
            user = userResponse != null ? userResponse.getData() : null;
            if (user == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppResponse<>("User does not exist", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AppResponse<>("Communication error with user service", null));
        }

        // Validar y obtener productos
        try {
            AppResponse<List<Product>> productResponse = webClientBuilder.build()
                .get()
                .uri(productUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<AppResponse<List<Product>>>() {})
                .block();
            products = productResponse != null ? productResponse.getData() : null;
            if (products == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppResponse<>("Products not found", null));
            }

            // Validar que los productos existen y guardarlos si es necesario
            List<Item> tempItems = order.getItems(); // Guardar los items temporalmente
            for (Item item : tempItems) {
                boolean exists = products.stream()
                    .anyMatch(p -> {
                        if (productService.getProductById(p.getId()) == null) {
                            System.out.println("Product does not exist, saving: " + p);
                            productService.saveProduct(p);
                        }
                        return p.getId().equals(item.getProduct().getId());
                    });
                if (!exists) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AppResponse<>("Product does not exist", null));
                }
                
                // Asegurar que el producto tiene toda la información necesaria
                Product fullProduct = products.stream()
                    .filter(p -> p.getId().equals(item.getProduct().getId()))
                    .findFirst()
                    .orElse(null);
                
                if (fullProduct != null) {
                    item.setProduct(fullProduct); // Usar el producto completo con todos los datos
                } else {
                    // Si no se encuentra en la lista, obtenerlo de la base de datos local
                    Product localProduct = productService.getProductById(item.getProduct().getId());
                    if (localProduct != null) {
                        item.setProduct(localProduct);
                    }
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AppResponse<>("Failed to retrieve product list", null));
        }
        userService.saveUser(user); // Ensure the user is saved or updated in the user service
        Order newOrder = Order.builder()
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
        
        for (Item item : order.getItems()) {
            // Verificar si el producto existe en la lista de productos
            item.setOrder(newOrder);
            System.out.println("Item product: " + item.getProduct());
            System.out.println("Item product price: " + (item.getProduct() != null ? item.getProduct().getPrice() : "null"));
        }
        System.out.println("New Order: " + newOrder);
        order.getItems().forEach(item -> {
            System.out.println("Item: " + item);
            System.out.println("Product: " + item.getProduct());
            System.out.println("Product Price: " + (item.getProduct() != null ? item.getProduct().getPrice() : "null"));
        });
        
        newOrder.setItems(new ArrayList<>(order.getItems())); // Set items from the request
        // Guardar la orden
        try{
            if (orderService.save(newOrder)) {
                return ResponseEntity.ok(new AppResponse<>("Order created successfully", newOrder));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new AppResponse<>("Failed to create order", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppResponse<>("Error saving order: " + e.getMessage(), null));
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
