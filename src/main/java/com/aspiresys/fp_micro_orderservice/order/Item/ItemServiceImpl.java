package com.aspiresys.fp_micro_orderservice.order.Item;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Override
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    @Override
    public Item getById(Long id) {
        Optional<Item> optionalItem = itemRepository.findById(id);
        return optionalItem.orElse(null);
    }

    @Override
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    @Override
    public List<Item> getItemsByOrderId(Long orderId) {
        return itemRepository.findByOrderId(orderId);
    }

    @Override
    public List<Item> getItemsByProductId(Long productId) {
        return itemRepository.findByProductId(productId);
    }

    @Override
    public boolean deleteByOrderId(Long orderId) {
        List<Item> items = itemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            return false;
        }
        itemRepository.deleteAll(items);
        return true;
    }

    @Override
    public boolean deleteById(Long id) {
        if (itemRepository.existsById(id)) {
            itemRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean update(Item item) {
        if (item.getId() == null || !itemRepository.existsById(item.getId())) {
            return false;
        }
        itemRepository.save(item);
        return true;
    }
}
