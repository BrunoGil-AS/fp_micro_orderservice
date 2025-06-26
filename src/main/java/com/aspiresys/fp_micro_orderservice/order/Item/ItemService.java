package com.aspiresys.fp_micro_orderservice.order.Item;

import java.util.List;

public interface ItemService {
    
    /**
     * Saves the given item.
     *
     * @param item the item to save
     * @return the saved item
     */
    Item save(Item item);

    /**
     * Retrieves an item by its ID.
     *
     * @param id the ID of the item to retrieve
     * @return the retrieved item, or null if not found
     */
    Item getById(Long id);

    /**
     * Retrieves all items.
     *
     * @return a list of all items
     */
    List<Item> getAllItems();


    /**
     * Gets a list of items by their order ID.
     * @param orderId the ID of the order to retrieve items for
     * @return a list of items associated with the specified order ID, or an empty list if none found
     */
    List<Item> getItemsByOrderId(Long orderId);

    /**
     * Get a list of items by their product ID.
     * 
     * @param productId the ID of the product to retrieve items for
     * @return a list of items associated with the specified product ID, or an empty list if none found
     */
    List<Item> getItemsByProductId(Long productId);

    /**
     * Deletes an item by order ID.
     * @param orderId the ID of the order to delete items for
     * @return boolean indicating whether the deletion was successful
     * @throws IllegalArgumentException if the orderId is null or does not exist
     */
    boolean deleteByOrderId(Long orderId);

    /**
     * Deletes an item by its ID.
     *
     * @param id the ID of the item to delete
     * @return true if the item was deleted, false otherwise
     */
    boolean deleteById(Long id);
    
    /**
     * Updates the given item.
     *
     * @param item the item to update
     * @return true if the item was updated successfully, false otherwise
     */
    boolean update(Item item);

    /**
     * Saves all items in the provided list.
     *
     * @param items the list of items to save
     */
    void saveAll(List<Item> items);
    
}
