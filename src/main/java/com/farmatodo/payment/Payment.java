package com.farmatodo.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "order_id")
    private UUID orderId;

    @Column(nullable = false, name = "token_id")
    private String tokenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(name = "tx_id")
    private String txId;

    public enum PaymentStatus {
        INITIATED,
        SUCCESS,
        FAILED
    }

    public Payment() {}

    public Payment(UUID orderId, String tokenId, PaymentStatus status, int attempts,
                  String lastError, Instant createdAt, String txId) {
        this.orderId = orderId;
        this.tokenId = tokenId;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.txId = txId;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getTokenId() { return tokenId; }
    public PaymentStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTxId() { return txId; }

    public void setStatus(PaymentStatus status) { this.status = status; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
