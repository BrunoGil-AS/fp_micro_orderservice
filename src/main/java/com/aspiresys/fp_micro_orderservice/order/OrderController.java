package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.web.bind.annotation.*;

import com.aspiresys.fp_micro_orderservice.common.dto.AppResponse;
import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import com.aspiresys.fp_micro_orderservice.order.Item.ItemService;
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
    private ItemService itemService;

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
    public ResponseEntity<AppResponse<Order>> createOrder(@RequestBody Order order) throws InterruptedException {
        // Validar existencia de usuario y producto a través del gateway
        String userUrl = "http://localhost:8080/user-service/users/find?email=" + order.getUser().getEmail();
        String productUrl = "http://localhost:8080/product-service/products"; // returns an AppResponse with a message and a list of products
        User user = null;
        List<Product> products = null;
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

        try {
            AppResponse<List<Product>> productResponse = webClientBuilder.build()
                .get()
                .uri(productUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<AppResponse<List<Product>>>() {})
                .block();
            products= productResponse != null ? productResponse.getData() : null;
            if (products == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppResponse<>("Products not found", null));
            }
            //checks if the list of products from the order exists in the products list
            for (Item item : order.getItems()) {
                boolean exists = products.stream()
                    .anyMatch(p -> {
                        if (productService.getProductById(p.getId()) == null) {
                            System.out.println("Product does not exist, saving: " + p);
                            productService.saveProduct(p); // Save the product if it does not exist
                        }
                        return p.getId().equals(item.getProduct().getId());
                    });
                if (!exists) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AppResponse<>("Product does not exist", null));
                }
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AppResponse<>("Failed to retrieve product list", null));
        }
        userService.saveUser(user); // Ensure the user is saved or updated in the user service
        
        order.setUser(user); // Set the user from the retrieved user object
        order.setItemsFromProducts(products); // Set items from the retrieved products list
        //System.out.println("\n\nOrder to save: " + order+"\n\n");
        //Thread.sleep(30000);
        order.setCreatedAt(LocalDateTime.now()); // Set the creation date
        //itemService.saveAll(order.getItems()); // Save all items in the order
        boolean saved = orderService.save(order);
        //System.out.println("\n\nOrder to save: " + order+"\n\n");
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
