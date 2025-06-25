package com.aspiresys.fp_micro_orderservice.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for managing products.
 * <p>
 * This class provides methods to save, retrieve, and delete products
 * by interacting with the {@link ProductRepository}.
 * </p>
 *
 * <p>
 * The following operations are supported:
 * <ul>
 *   <li>Save a new product</li>
 *   <li>Retrieve all products</li>
 *   <li>Retrieve a product by its ID</li>
 *   <li>Delete a product by its ID</li>
 * </ul>
 * </p>
 *
 * @author bruno.gil
 * @see ProductService
 * @see ProductRepository
 */
@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Override
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.orElse(null);
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
