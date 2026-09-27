package com.craftora.craftora_backend.repository;

import com.craftora.craftora_backend.model.CustomerCartItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCartItemRepository extends JpaRepository<CustomerCartItem, Long> {
    List<CustomerCartItem> findAllByCustomerId(Long customerId);
    void deleteAllByCustomerId(Long customerId);
    void deleteAllByProductId(Long productId);
}
