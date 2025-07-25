package com.aspiresys.fp_micro_orderservice.product;

import com.aspiresys.fp_micro_orderservice.kafka.dto.ProductMessage;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing products synchronized from Product Service via Kafka.
 * Uses the existing Product entity and ProductRepository.
 * 
 * @author bruno.gil
 */
@Service
@Log
public class ProductSyncService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * Save or update a product from Kafka message
     * 
     * @param productMessage Product message from Kafka
     */
    public void saveOrUpdateProduct(ProductMessage productMessage) {
        try {
            log.info("SYNC: Checking if product ID " + productMessage.getId() + " exists in database...");
            Optional<Product> existingProduct = productRepository.findById(productMessage.getId());
            
            if (existingProduct.isPresent()) {
                // Update existing product
                log.info("SYNC: Product ID " + productMessage.getId() + " exists, updating...");
                Product product = existingProduct.get();
                updateProductFromMessage(product, productMessage);
                Product savedProduct = productRepository.save(product);
                log.info("SYNC: Updated product ID " + savedProduct.getId() + 
                         " (" + savedProduct.getName() + ") - Event: " + productMessage.getEventType());
            } else {
                // Create new product
                log.info("SYNC: Product ID " + productMessage.getId() + " does not exist, creating new...");
                Product newProduct = createProductFromMessage(productMessage);
                Product savedProduct = productRepository.save(newProduct);
                log.info("SYNC: Created new product ID " + savedProduct.getId() + 
                         " (" + savedProduct.getName() + ") - Event: " + productMessage.getEventType());
                log.info("SYNC: Total products in database: " + productRepository.count());
            }
        } catch (Exception e) {
            log.severe("SYNC ERROR: Failed to save/update product ID " + productMessage.getId() + 
                      " (" + productMessage.getName() + ") - Error: " + e.getMessage());
            log.severe("SYNC ERROR: Event type was: " + productMessage.getEventType());
            e.printStackTrace();
        }
    }

    /**
     * Delete a product by ID
     * 
     * @param productId Product ID to delete
     */
    public void deleteProduct(Long productId) {
        try {
            if (productRepository.existsById(productId)) {
                productRepository.deleteById(productId);
                log.info("SYNC: Deleted product ID " + productId + " - Event: PRODUCT_DELETED");
            } else {
                log.warning("SYNC: Attempted to delete non-existent product ID " + productId);
            }
        } catch (Exception e) {
            log.severe("SYNC ERROR: Failed to delete product ID " + productId + " - Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get all products
     * 
     * @return List of all products
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Get product by ID
     * 
     * @param productId Product ID
     * @return Optional of Product
     */
    public Optional<Product> getProductById(Long productId) {
        return productRepository.findById(productId);
    }

    /**
     * Check if product has sufficient stock
     * 
     * @param productId Product ID
     * @param requiredQuantity Required quantity
     * @return true if sufficient stock available
     */
    public boolean hasSufficientStock(Long productId, Integer requiredQuantity) {
        Optional<Product> product = productRepository.findById(productId);
        return product.isPresent() && product.get().getStock() >= requiredQuantity;
    }

    /**
     * Update stock after order
     * 
     * @param productId Product ID
     * @param quantity Quantity to reduce
     * @return true if stock was updated successfully
     */
    public boolean reduceStock(Long productId, Integer quantity) {
        try {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                if (product.getStock() >= quantity) {
                    product.setStock(product.getStock() - quantity);
                    productRepository.save(product);
                    log.info("Reduced stock for product " + productId + " by " + quantity);
                    return true;
                } else {
                    log.warning("Insufficient stock for product " + productId);
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            log.severe("Error reducing stock: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get count of products
     * 
     * @return Total number of products
     */
    public long getProductCount() {
        return productRepository.count();
    }

    /**
     * Log synchronization statistics
     * Useful for monitoring the sync status
     */
    public void logSyncStatistics() {
        long totalProducts = productRepository.count();
        log.info("SYNC STATS: Total synchronized products: " + totalProducts);
    }

    /**
     * Creates a new Product from ProductMessage
     */
    private Product createProductFromMessage(ProductMessage message) {
        Product product = new Product();
        product.setId(message.getId());
        updateProductFromMessage(product, message);
        return product;
    }

    /**
     * Updates an existing Product with data from ProductMessage
     */
    private void updateProductFromMessage(Product product, ProductMessage message) {
        product.setName(message.getName());
        product.setPrice(message.getPrice());
        product.setCategory(message.getCategory());
        product.setImageUrl(message.getImageUrl());
        product.setStock(message.getStock());
        product.setBrand(message.getBrand());
    }

    /**
     * Triggers a request for product synchronization.
     * This method logs a request for manual synchronization.
     * 
     * Note: Actual synchronization happens automatically via Kafka consumer.
     * Use the Product Service admin endpoint /admin/kafka/sync/force-full-sync 
     * to trigger a complete synchronization.
     */
    public void requestProductSynchronization() {
        long currentCount = getProductCount();
        log.info("SYNC REQUEST: Current products: " + currentCount + 
                 ". Kafka consumer is listening for new messages.");
        log.info("TIP: Use Product Service endpoint POST /admin/kafka/sync/force-full-sync to trigger full sync");
    }

    /**
     * Checks if the product database appears to be synchronized.
     * Returns true if there are products, false if empty.
     * 
     * @return true if products exist, false if database appears empty
     */
    public boolean isProductDatabaseSynchronized() {
        long count = getProductCount();
        boolean isSynced = count > 0;
        
        if (!isSynced) {
            log.warning("Product database appears empty. Consider triggering synchronization from Product Service.");
        } else {
            log.info("Product database has " + count + " products synchronized.");
        }
        
        return isSynced;
    }
}
