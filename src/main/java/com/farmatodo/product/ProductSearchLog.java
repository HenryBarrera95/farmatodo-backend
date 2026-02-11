package com.farmatodo.product;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_search_logs")
public class ProductSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "min_stock")
    private int minStock;

    @Column(name = "tx_id")
    private String txId;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    public ProductSearchLog() {}

    public ProductSearchLog(int minStock, String txId, Instant createdAt) {
        this.minStock = minStock;
        this.txId = txId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public int getMinStock() { return minStock; }
    public String getTxId() { return txId; }
    public Instant getCreatedAt() { return createdAt; }
}
