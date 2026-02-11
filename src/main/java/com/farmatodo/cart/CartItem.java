package com.farmatodo.cart;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "cart_id")
    private Cart cart;

    @Column(nullable = false, name = "product_id")
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, name = "unit_price_snapshot", precision = 19, scale = 2)
    private BigDecimal unitPriceSnapshot;

    public CartItem() {}

    public CartItem(Cart cart, UUID productId, int quantity, BigDecimal unitPriceSnapshot) {
        this.cart = cart;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public UUID getId() { return id; }
    public Cart getCart() { return cart; }
    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public BigDecimal getSubtotal() { return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity)); }

    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }
}
