package com.aspiresys.fp_micro_orderservice.order.dto;

import com.aspiresys.fp_micro_orderservice.order.Order;
import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import com.aspiresys.fp_micro_orderservice.product.Product;
import com.aspiresys.fp_micro_orderservice.user.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class for converting between Order entities and DTOs.
 * <p>
 * This class provides methods to convert Order entities to DTOs and vice versa,
 * helping to avoid circular references and excessive nesting in JSON serialization.
 * </p>
 *
 * @author bruno.gil
 */
@Component
public class OrderMapper {

    /**
     * Converts an Order entity to OrderDTO.
     *
     * @param order the Order entity to convert
     * @return OrderDTO representation of the order
     */
    public OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }

        return OrderDTO.builder()
                .id(order.getId())
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .items(order.getItems() != null ? 
                    order.getItems().stream()
                        .map(this::toItemDTO)
                        .collect(Collectors.toList()) : 
                    new ArrayList<>())
                .createdAt(order.getCreatedAt())
                .total(order.getTotal())
                .build();
    }

    /**
     * Converts an Item entity to ItemDTO.
     *
     * @param item the Item entity to convert
     * @return ItemDTO representation of the item
     */
    public ItemDTO toItemDTO(Item item) {
        if (item == null) {
            return null;
        }

        return ItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                .productPrice(item.getProduct() != null ? item.getProduct().getPrice() : null)
                .productCategory(item.getProduct() != null ? item.getProduct().getCategory() : null)
                .productImageUrl(item.getProduct() != null ? item.getProduct().getImageUrl() : null)
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    /**
     * Converts a CreateOrderDTO to Order entity.
     *
     * @param createOrderDTO the CreateOrderDTO to convert
     * @param user the User entity to associate with the order
     * @return Order entity representation
     */
    public Order toEntity(CreateOrderDTO createOrderDTO, User user) {
        if (createOrderDTO == null) {
            return null;
        }

        Order order = Order.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        if (createOrderDTO.getItems() != null) {
            List<Item> items = createOrderDTO.getItems().stream()
                    .map(createItemDTO -> toItemEntity(createItemDTO, order))
                    .collect(Collectors.toList());
            order.setItems(items);
        }

        return order;
    }

    /**
     * Converts a CreateItemDTO to Item entity.
     *
     * @param createItemDTO the CreateItemDTO to convert
     * @param order the Order entity to associate with the item
     * @return Item entity representation
     */
    private Item toItemEntity(CreateOrderDTO.CreateItemDTO createItemDTO, Order order) {
        if (createItemDTO == null) {
            return null;
        }

        // Create a Product entity with just the ID
        // The actual product details will be fetched and validated by the service
        Product product = new Product();
        product.setId(createItemDTO.getProductId());

        return Item.builder()
                .order(order)
                .product(product)
                .quantity(createItemDTO.getQuantity())
                .build();
    }

    /**
     * Converts a list of Order entities to a list of OrderDTOs.
     *
     * @param orders the list of Order entities to convert
     * @return list of OrderDTO representations
     */
    public List<OrderDTO> toDTOList(List<Order> orders) {
        if (orders == null) {
            return new ArrayList<>();
        }

        return orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
