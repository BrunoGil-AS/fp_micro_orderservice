package com.aspiresys.fp_micro_orderservice.order.dto;

import com.aspiresys.fp_micro_orderservice.order.Order;
import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import org.springframework.stereotype.Component;

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
