package com.aspiresys.fp_micro_orderservice.product;

import java.util.List;

import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

/**
 * Abstract base class representing a product entity.
 * <p>
 * This class is mapped as a JPA entity with joined inheritance strategy,
 * allowing subclasses to represent specific types of products.
 * </p>
 *
 * Fields:
 * <ul>
 *   <li><b>id</b>: Unique identifier for the product (auto-generated).</li>
 *   <li><b>stock</b>: Quantity of the product available in inventory.</li>
 *   <li><b>name</b>: Name of the product.</li>
 *   <li><b>price</b>: Price of the product.</li>
 *   <li><b>category</b>: Category to which the product belongs.</li>
 *   <li><b>imageUrl</b>: URL of the product's image.</li>
 * </ul>
 *
 * Annotations:
 * <ul>
 *   <li>{@code @Entity}: Marks this class as a JPA entity.</li>
 *   <li>Lombok annotations for getters, setters, constructors.</li>
 * </ul>
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Product {
    @Id
    private Long id;

    private int stock;
    private String name;
    private Double price;
    private String category;
    private String imageUrl;
    @JsonIgnore
    @OneToMany(mappedBy = "product")
    private List<Item> Items;
    
    /**
     * Gets the price of the product.
     * Returns 0.0 if price is null to avoid NullPointerException.
     * @return the price of the product, or 0.0 if null
     */
    public Double getPrice() {
        return price != null ? price : 0.0;
    }
}
