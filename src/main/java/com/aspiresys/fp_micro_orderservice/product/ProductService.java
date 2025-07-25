package com.aspiresys.fp_micro_orderservice.product;

import java.util.List;

/**
 * Service interface for managing products.
 * Provides methods for saving, retrieving, and deleting products.
 */
public interface ProductService {
    Product saveProduct(Product product);
    List<Product> getAllProducts();
    Product getProductById(Long id);
    void deleteProduct(Long id);
    /**
     * update the stock with subtracting the quantity to the current product
     * stock.
     * @param productId of the target product
     * @param quantity of items
     */
    void updateProductStock(Long productId, int quantity);
}
