package com.farmatodo.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "customer_id")
    private UUID customerId;

    @Column(nullable = false, name = "cart_id")
    private UUID cartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(nullable = false, name = "token_id")
    private String tokenId;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(name = "tx_id")
    private String txId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public enum OrderStatus {
        CREATED,
        PAYMENT_PENDING,
        PAID,
        PAYMENT_FAILED,
        CANCELLED
    }

    public Order() {}

    public Order(UUID customerId, UUID cartId, OrderStatus status, BigDecimal totalAmount,
                 String deliveryAddress, String tokenId, Instant createdAt, String txId) {
        this.customerId = customerId;
        this.cartId = cartId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.deliveryAddress = deliveryAddress;
        this.tokenId = tokenId;
        this.createdAt = createdAt;
        this.txId = txId;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public UUID getCartId() { return cartId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getTokenId() { return tokenId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTxId() { return txId; }
    public List<OrderItem> getItems() { return items; }

    public void setStatus(OrderStatus status) { this.status = status; }
}
