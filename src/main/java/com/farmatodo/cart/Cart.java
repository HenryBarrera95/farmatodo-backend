package com.farmatodo.cart;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CartStatus status;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public enum CartStatus {
        ACTIVE,
        ORDERED
    }

    public Cart() {}

    public Cart(UUID customerId, CartStatus status, Instant createdAt) {
        this.customerId = customerId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public CartStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<CartItem> getItems() { return items; }

    public void setStatus(CartStatus status) { this.status = status; }
}
