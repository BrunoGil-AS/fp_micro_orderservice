package com.aspiresys.fp_micro_orderservice.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.java.Log;

// AOP imports
import com.aspiresys.fp_micro_orderservice.aop.annotation.Auditable;
import com.aspiresys.fp_micro_orderservice.aop.annotation.ExecutionTime;
import com.aspiresys.fp_micro_orderservice.aop.annotation.ValidateParameters;
import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import com.aspiresys.fp_micro_orderservice.product.Product;
import com.aspiresys.fp_micro_orderservice.product.ProductService;
import com.aspiresys.fp_micro_orderservice.product.ProductSyncService;
import com.aspiresys.fp_micro_orderservice.user.User;
import com.aspiresys.fp_micro_orderservice.user.UserService;
import com.aspiresys.fp_micro_orderservice.user.UserSyncService;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Service for validating orders using synchronized local data from Kafka consumers.
 * 
 * This service has been refactored to eliminate HTTP calls to external services.
 * Instead, it relies on:
 * - ProductConsumerService: Keeps product data synchronized via Kafka
 * - UserConsumerService: Keeps user data synchronized via Kafka
 * 
 * Benefits of Kafka-based synchronization:
 * - Better performance (no network calls during validation)
 * - Better reliability (no dependency on external service availability)
 * - Eventual consistency maintained automatically
 * - Real-time updates through Kafka messages
 * 
 * The validation process now:
 * 1. Validates users using local synchronized data
 * 2. Validates products using local synchronized data
 * 3. Processes order items with complete synchronized product information
 * 
 * If data is not found locally, it suggests checking Kafka synchronization status.
 * 
 * @author bruno.gil
 */
@Service
@Log
public class OrderValidationService {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductSyncService productSyncService;
    
    @Autowired
    private UserSyncService userSyncService;

    /**
     * Validates user existence using local synchronized data from Kafka consumers.
     * If user is not found locally, it suggests checking if user sync is up to date.
     * 
     * @param email User email to validate
     * @return CompletableFuture with the validated User or null if not found
     */
    @Async("orderValidationExecutor")
    @Auditable(operation = "VALIDATE_USER_ASYNC", entityType = "User", logParameters = true)
    @ExecutionTime(operation = "Validate User Async", warningThreshold = 500, detailed = true)
    @ValidateParameters(notNull = true, notEmpty = true, message = "Email cannot be null or empty")
    public CompletableFuture<User> validateUserAsync(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("KAFKA SYNC VALIDATION: Validating user from synchronized local data: " + email);
                
                // Get user from local synchronized database
                User localUser = userService.getUserByEmail(email);
                
                if (localUser != null) {
                    log.info("KAFKA SYNC VALIDATION: User found in local synchronized database: " + email);
                    return localUser;
                } else {
                    log.warning("KAFKA SYNC VALIDATION: User not found in local database: " + email);
                    log.info("KAFKA SYNC INFO: Total synchronized users: " + userSyncService.getSynchronizedUserCount());
                    
                    throw new ValidationException("User not found in synchronized database: " + email + 
                        ". Please verify user exists in User Service and Kafka synchronization is working.");
                }
                
            } catch (ValidationException e) {
                log.warning("KAFKA SYNC VALIDATION: User validation failed: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                log.severe("KAFKA SYNC VALIDATION: Unexpected error during user validation: " + e.getMessage());
                e.printStackTrace();
                throw new ValidationException("Error accessing synchronized user data: " + e.getMessage());
            }
        });
    }

    /**
     * Validates products existence using synchronized local data from Kafka consumers.
     * Ensures all products in the order items exist in the local synchronized database.
     * 
     * @param items Order items to validate product existence
     * @return CompletableFuture with the list of products or throws exception if validation fails
     */
    @Async("orderValidationExecutor")
    @Auditable(operation = "VALIDATE_PRODUCTS_ASYNC", entityType = "Product", logParameters = true)
    @ExecutionTime(operation = "Validate Products Async", warningThreshold = 1000, detailed = true)
    @ValidateParameters(notNull = true, notEmpty = true, message = "Items list cannot be null or empty")
    public CompletableFuture<List<Product>> validateProductsAsync(List<Item> items) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("KAFKA SYNC VALIDATION: Validating " + items.size() + " products from synchronized local data");
                
                // Check if product database is synchronized
                if (!productSyncService.isProductDatabaseSynchronized()) {
                    log.warning("KAFKA SYNC WARNING: Product database may not be fully synchronized");
                    productSyncService.requestProductSynchronization();
                }
                
                List<Product> validatedProducts = new ArrayList<>();
                StringBuilder missingProducts = new StringBuilder();
                
                for (Item item : items) {
                    Long productId = item.getProduct().getId();
                    Product localProduct = productService.getProductById(productId);
                    
                    if (localProduct != null) {
                        validatedProducts.add(localProduct);
                        log.info("KAFKA SYNC VALIDATION: Product found - ID: " + productId + ", Name: " + localProduct.getName());
                    } else {
                        missingProducts.append("Product ID ").append(productId).append(" not found in synchronized database. ");
                        log.warning("KAFKA SYNC VALIDATION: Product missing - ID: " + productId);
                    }
                }
                
                if (missingProducts.length() > 0) {
                    String errorMessage = missingProducts.toString() + 
                        "Please verify products exist in Product Service and Kafka synchronization is working.";
                    log.warning("KAFKA SYNC VALIDATION: " + errorMessage);
                    throw new ValidationException(errorMessage);
                }
                
                log.info("KAFKA SYNC VALIDATION: All " + validatedProducts.size() + " products validated successfully");
                return validatedProducts;
                
            } catch (ValidationException e) {
                log.warning("KAFKA SYNC VALIDATION: Product validation failed: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                log.severe("KAFKA SYNC VALIDATION: Unexpected error during product validation: " + e.getMessage());
                e.printStackTrace();
                throw new ValidationException("Error accessing synchronized product data: " + e.getMessage());
            }
        });
    }

    /**
     * Validates and processes order items with synchronized local products.
     * This method now ensures items use complete product data from synchronized database.
     * 
     * @param items Order items to validate and process
     * @param products Validated products from the synchronized database
     * @throws ValidationException if any product validation fails
     */
    @ExecutionTime(operation = "Validate and Process Items")
    @ValidateParameters(notNull = true, message = "Items and products cannot be null")
    public void validateAndProcessItems(List<Item> items, List<Product> products) {
        log.info("KAFKA SYNC PROCESSING: Processing " + items.size() + " order items with synchronized products");
        
        for (Item item : items) {
            Long productId = item.getProduct().getId();
            
            // Find the complete product data from validated products
            Product fullProduct = products.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .orElse(null);
            
            if (fullProduct != null) {
                // Use complete product with all synchronized data
                item.setProduct(fullProduct);
                log.info("KAFKA SYNC PROCESSING: Item updated with complete product data - ID: " + productId + 
                        ", Name: " + fullProduct.getName() + ", Price: " + fullProduct.getPrice());
            } else {
                // This should not happen since products were already validated
                log.severe("KAFKA SYNC PROCESSING ERROR: Product not found in validated list - ID: " + productId);
                throw new ValidationException("Product validation inconsistency for ID: " + productId);
            }
        }
        
        log.info("KAFKA SYNC PROCESSING: All " + items.size() + " items processed successfully");
    }

    /**
     * Validates the complete order asynchronously using synchronized local data.
     * This method runs user and product validation in parallel for better performance.
     * 
     * @param order Order to validate
     * @return OrderValidationResult containing the validated user and processed items
     */
    @ExecutionTime(operation = "Validate Complete Order", warningThreshold = 3000, detailed = true)
    @ValidateParameters(notNull = true, message = "Order cannot be null")
    public CompletableFuture<OrderValidationResult> validateOrderAsync(Order order) {
        log.info("KAFKA SYNC ORDER VALIDATION: Starting async validation for order with " + 
                order.getItems().size() + " items for user: " + order.getUser().getEmail());
        
        // Start both validations in parallel using synchronized data
        CompletableFuture<User> userFuture = validateUserAsync(order.getUser().getEmail());
        CompletableFuture<List<Product>> productsFuture = validateProductsAsync(order.getItems());

        // Combine results when both complete
        return CompletableFuture.allOf(userFuture, productsFuture)
            .thenApply(v -> {
                try {
                    User user = userFuture.join();
                    List<Product> products = productsFuture.join();
                    
                    log.info("KAFKA SYNC ORDER VALIDATION: Both user and products validated, processing items");
                    
                    // Validate and process items with synchronized data
                    validateAndProcessItems(order.getItems(), products);
                    
                    log.info("KAFKA SYNC ORDER VALIDATION: Order validation completed successfully");
                    return new OrderValidationResult(user, products, true, null);
                    
                } catch (Exception e) {
                    log.warning("KAFKA SYNC ORDER VALIDATION: Order validation failed: " + e.getMessage());
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
