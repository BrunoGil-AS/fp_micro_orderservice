package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.java.Log;

import com.aspiresys.fp_micro_orderservice.common.dto.AppResponse;
import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import com.aspiresys.fp_micro_orderservice.product.Product;
import com.aspiresys.fp_micro_orderservice.product.ProductService;
import com.aspiresys.fp_micro_orderservice.user.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for validating orders asynchronously with external services.
 * 
 * @author bruno.gil
 */
@Service
@Log
public class OrderValidationService {

    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Autowired
    private ProductService productService;

    /**
     * Validates user existence asynchronously
     * 
     * @param email User email to validate
     * @return CompletableFuture with the validated User or null if not found
     */
    @Async("orderValidationExecutor")
    public CompletableFuture<User> validateUserAsync(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Usar localhost directo evitando pasar por el gateway
                String userUrl = "http://localhost:9001/users/find?email=" + email;
                log.info("Attempting to validate user with URL: " + userUrl);
                
                AppResponse<User> userResponse = webClientBuilder.build()
                    .get()
                    .uri(userUrl)
                    .header("X-Internal-Service", "internal-secret-key-2024") // Header de seguridad para identificar servicio interno
                    .header("User-Agent", "order-service") // Identificar el servicio que hace la petición
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AppResponse<User>>() {})
                    .block();
                
                User user = userResponse != null ? userResponse.getData() : null;
                log.info("User validation response: " + (user != null ? "User found" : "User not found"));
                
                if (user == null) {
                    log.warning("User with email " + email + " does not exist in the user service.");
                    throw new ValidationException("User does not exist");
                }
                
                return user;
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                log.warning("Error communicating with user service: " + e.getMessage());
                e.printStackTrace(); // Para más detalles del error
                throw new ValidationException("Communication error with user service");
            }
        });
    }

    /**
     * Validates products existence asynchronously
     * 
     * @param items Order items to validate product existence
     * @return CompletableFuture with the list of products or throws exception if validation fails
     */
    @Async("orderValidationExecutor")
    public CompletableFuture<List<Product>> validateProductsAsync(List<Item> items) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Usar localhost directo al puerto del Product Service
                String productUrl = "http://localhost:9002/products";
                log.info("Attempting to validate products with URL: " + productUrl);
                
                AppResponse<List<Product>> productResponse = webClientBuilder.build()
                    .get()
                    .uri(productUrl)
                    .header("User-Agent", "order-service") // Identificar el servicio que hace la petición
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AppResponse<List<Product>>>() {})
                    .block();
                
                List<Product> products = productResponse != null ? productResponse.getData() : null;
                log.info("Product validation response: " + (products != null ? products.size() + " products found" : "No products found"));
                
                if (products == null) {
                    log.warning("Products not found in the product service.");
                    throw new ValidationException("Products not found");
                }
                
                return products;
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                log.warning("Error communicating with product service: " + e.getMessage());
                e.printStackTrace(); // Para más detalles del error
                throw new ValidationException("Failed to retrieve product list");
            }
        });
    }

    /**
     * Validates and processes order items with the retrieved products
     * 
     * @param items Order items to validate
     * @param products Available products from the product service
     * @throws ValidationException if any product doesn't exist
     */
    public void validateAndProcessItems(List<Item> items, List<Product> products) {
        for (Item item : items) {
            boolean exists = products.stream()
                .anyMatch(p -> {
                    // Save product locally if it doesn't exist
                    if (productService.getProductById(p.getId()) == null) {
                        System.out.println("Product does not exist locally, saving: " + p);
                        productService.saveProduct(p);
                    }
                    return p.getId().equals(item.getProduct().getId());
                });
            
            if (!exists) {
                throw new ValidationException("Product does not exist");
            }
            
            // Ensure the product has all necessary information
            Product fullProduct = products.stream()
                .filter(p -> p.getId().equals(item.getProduct().getId()))
                .findFirst()
                .orElse(null);
            
            if (fullProduct != null) {
                item.setProduct(fullProduct); // Use complete product with all data
            } else {
                // If not found in the list, get it from local database
                Product localProduct = productService.getProductById(item.getProduct().getId());
                if (localProduct != null) {
                    item.setProduct(localProduct);
                }
            }
        }
    }

    /**
     * Validates the complete order asynchronously
     * 
     * @param order Order to validate
     * @return OrderValidationResult containing the validated user and processed items
     */
    public CompletableFuture<OrderValidationResult> validateOrderAsync(Order order) {
        // Start both validations in parallel
        CompletableFuture<User> userFuture = validateUserAsync(order.getUser().getEmail());
        CompletableFuture<List<Product>> productsFuture = validateProductsAsync(order.getItems());

        // Combine results when both complete
        return CompletableFuture.allOf(userFuture, productsFuture)
            .thenApply(v -> {
                try {
                    User user = userFuture.join();
                    List<Product> products = productsFuture.join();
                    
                    // Validate and process items
                    validateAndProcessItems(order.getItems(), products);
                    
                    return new OrderValidationResult(user, products, true, null);
                } catch (Exception e) {
                    return new OrderValidationResult(null, null, false, e.getMessage());
                }
            });
    }

    /**
     * Custom exception for validation errors
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * Result object for order validation
     */
    public static class OrderValidationResult {
        private final User user;
        private final List<Product> products;
        private final boolean valid;
        private final String errorMessage;

        public OrderValidationResult(User user, List<Product> products, boolean valid, String errorMessage) {
            this.user = user;
            this.products = products;
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public User getUser() { return user; }
        public List<Product> getProducts() { return products; }
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
