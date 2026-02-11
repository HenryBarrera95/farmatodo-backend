package com.farmatodo.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @Query("SELECT i FROM CartItem i WHERE i.cart.id = :cartId AND i.productId = :productId")
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);
}
