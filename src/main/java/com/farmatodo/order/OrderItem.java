package com.farmatodo.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "order_id")
    private Order order;

    @Column(nullable = false, name = "product_id")
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, name = "unit_price_snapshot", precision = 19, scale = 2)
    private BigDecimal unitPriceSnapshot;

    public OrderItem() {}

    public OrderItem(Order order, UUID productId, int quantity, BigDecimal unitPriceSnapshot) {
        this.order = order;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public UUID getId() { return id; }
    public Order getOrder() { return order; }
    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public BigDecimal getSubtotal() { return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity)); }
}
